package me.kavyamurthy.hestia

import Conversation
import ConversationType
import CreateUserReq
import LoginRequest
import LoginResponse
import Message
import VerifyEmail
import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import me.kavyamurthy.hestia.db.DirectConversations
import me.kavyamurthy.hestia.db.DirectMessages
import me.kavyamurthy.hestia.db.Groups
import me.kavyamurthy.hestia.db.Login.login
import me.kavyamurthy.hestia.db.Login.loginExists
import me.kavyamurthy.hestia.db.Messages
import me.kavyamurthy.hestia.db.Spaces
import me.kavyamurthy.hestia.db.Users
import me.kavyamurthy.hestia.db.Users.createUser
import me.kavyamurthy.hestia.db.Users.userExists
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections

private val logger = LoggerFactory.getLogger("HestiaServer")
private val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

fun main() {
    Database.connect(config.db.url, user = config.db.username, password = config.db.password)

    logger.info("Starting server")
    embeddedServer(Netty, port = config.server.port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                explicitNulls = false
            }
        )
    }
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT.require(jwtAlgorithm).build())
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }

    }
    install(CallLogging)


    routing {

        staticResources("/", "website", index = "index.html")

        post("/api/login") {
            try {
                val loginRequest = call.receive<LoginRequest>()
                val user = login(loginRequest.email, hashPassword(loginRequest.password))
                val tokenExpiry = Instant.now().plus(7, ChronoUnit.DAYS)
                if (user != null) {
                    logger.info("Login success $user")
                    val token = JWT.create()
                        .withClaim("userId", user.id)
                        .withExpiresAt(tokenExpiry)
                        .sign(jwtAlgorithm)
                    call.respond(
                        LoginResponse(
                            token,
                            user.displayName,
                            tokenExpiry.epochSecond
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.Forbidden)
                }
            } catch (e: Exception) {
                logger.error("Error in login", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/api/sendCode") {
            val email = call.receive<String>()
            if(loginExists(email)) {
                call.respond(HttpStatusCode.Conflict)
            } else {
                sendCode(email)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/api/verifyCode") {
            val verifyCode = call.receive<VerifyEmail>()
            if(verifyCode(verifyCode.email, verifyCode.code)) {
                val token = JWT.create()
                    .withClaim("email", verifyCode.email)
                    .withExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                    .sign(jwtAlgorithm)
                call.respond(token)
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        post("/api/signup") {
            try {
                val userReq = call.receive<CreateUserReq>()
                if(userExists(userReq.userName)) {
                    call.respond(HttpStatusCode.Conflict)
                } else {
                    createUser(userReq.userName, userReq.displayName, userReq.email, userReq.password)
                    call.respond(HttpStatusCode.OK)
                }
            } catch (e: Exception) {
                logger.error("Exception in signup", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        authenticate ("auth-jwt") {
            get("/api/groups") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupList = Groups.getGroupList(userId)
                call.respond(groupList)
            }

            get("/api/groups/{id}/members") {
                val groupId = call.parameters["id"]?.toLong()
                    ?: throw IllegalArgumentException("Group ID not specified")
                val members = Groups.getMembers(groupId)
                call.respond(members)
            }

            post("/api/groups/{id}/members") {
                val groupId = call.parameters["id"]?.toLong()
                    ?: throw IllegalArgumentException("Group ID not specified")

                val otherUserName = call.receive<String>()
                val otherUser = Users.lookupUser(otherUserName)
                if (otherUser == null) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    Groups.addMember(groupId, otherUser.id)
                }
                call.respond(HttpStatusCode.OK)
            }

            get("/api/spaces") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val spaceList = Spaces.getSpaces(userId)
                call.respond(spaceList)
            }

            get("/api/conversations") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val convList = DirectConversations.getConvList(userId)
                call.respond(convList)
            }

            post("/api/newConversation") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val otherUserName = call.receive<String>()
                val otherUser = Users.lookupUser(otherUserName)
                if (otherUser == null) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    val convId = DirectConversations.getOrAddConv(userId, otherUser.id)
                    call.respond(Conversation(otherUser.displayName, convId, ConversationType.DIRECT))
                }
            }

            get("/api/conversation/{type}/{id}") {
                try {
//                    val principal = call.principal<JWTPrincipal>()
//                    val userId = principal!!.payload.getClaim("userId").asInt()
                    val convType = call.parameters["type"]
                        ?.let { ConversationType.valueOf(it) }
                        ?: throw IllegalArgumentException("Conversation type not specified")

                    val convId = call.parameters["id"]?.toLong()
                        ?: throw IllegalArgumentException("Conversation ID not specified")

                    val messages = when (convType) {
                        ConversationType.DIRECT -> DirectMessages.getMessages(convId)
                        ConversationType.SPACE ->  Messages.getMessages(convId)
                    }
                    call.respond(messages)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Illegal argument")
                }
            }

            webSocket("/chat/{id}") {
                try {
                    chatConnection(this)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Illegal argument")
                }
            }
        }
    }
}

private suspend fun Route.chatConnection(session: WebSocketServerSession) {
    val principal = session.call.principal<JWTPrincipal>() ?: throw IllegalArgumentException("Auth principal not found")
    val userId = principal.payload.getClaim("userId").asInt()
    val thisConnection = Connection(session, userId)
    connections += thisConnection

    val convId = session.call.parameters["id"]?.toLong() ?: throw IllegalArgumentException("Conversation ID not specified")



    try {
        while (true) {
            val msg = session.receiveDeserialized<Message>()
            logger.info(msg.convType.toString())

            val userList = when(msg.convType) {
                ConversationType.DIRECT -> DirectConversations.getMembers(convId)
                ConversationType.SPACE ->  Spaces.getMembers(convId)
            }.filter { it != userId }.toMutableSet()

            logger.info(userList.toString())

            connections.forEach {
                if (it.userId in userList)
                    (it.session as WebSocketServerSession).sendSerialized(msg)
                when (msg.convType) {
                    ConversationType.DIRECT -> DirectMessages.add(userId, msg)
                    ConversationType.SPACE -> Messages.add(userId, msg)
                }
            }
        }
    } catch (_: ClosedReceiveChannelException) {
    } catch (e: Exception) {
        logger.error("Error in chat", e)
    } finally {
        connections -= thisConnection
    }
}