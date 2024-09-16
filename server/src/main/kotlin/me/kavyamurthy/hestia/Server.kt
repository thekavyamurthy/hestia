package me.kavyamurthy.hestia

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
import me.kavyamurthy.hestia.db.ConversationMembers
import me.kavyamurthy.hestia.db.Conversations
import me.kavyamurthy.hestia.db.DirectMessages
import me.kavyamurthy.hestia.db.Login.login
import me.kavyamurthy.hestia.db.Login.loginExists
import me.kavyamurthy.hestia.db.Users.createUser
import me.kavyamurthy.hestia.db.Users.userExists
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections

private val logger = LoggerFactory.getLogger("HestiaServer")

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
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

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
            get("/api/conversations") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val convList = Conversations.getConvList(userId)
                call.respond(convList)
            }

            get("/api/conversation/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "conversation id not specified")
                } else {
                    call.respond(DirectMessages.getMessages(id))
                }
            }

            webSocket("/chat/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val thisConnection = Connection(this, userId)
                connections += thisConnection
                val convId = call.parameters["id"]?.toLong()

                if (convId == null) {
                    call.respond(HttpStatusCode.BadRequest, "conversation id not specified")
                } else {
                    val userList = ConversationMembers.getMembers(convId).toMutableSet()
                    userList.remove(userId)

                    try {
                        while (true) {
                            val msg = receiveDeserialized<Message>()
                            connections.forEach {
                                if (it.userId in userList)
                                    (it.session as WebSocketServerSession).sendSerialized(msg)
                                DirectMessages.add(userId, msg)
                            }
                        }
                    } catch (_: ClosedReceiveChannelException) {
                    } catch (e: Exception) {
                        logger.error("Error in chat", e)
                    } finally {
                        connections -= thisConnection
                    }
                }
            }
        }
    }
}