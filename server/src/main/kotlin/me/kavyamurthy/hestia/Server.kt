package me.kavyamurthy.hestia

import LoginRequest
import LoginResponse
import Message
import SERVER_PORT
import com.auth0.jwt.JWT
import io.ktor.http.ContentType
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
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
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
import me.kavyamurthy.hestia.db.Users.login
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections



fun main() {
    Database.connect(config.db.url, user = config.db.username, password = config.db.password)

    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
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
        get("/") {
            call.respondText("Test", ContentType.Text.Plain)
        }

        post("/api/login") {
            val loginRequest = call.receive<LoginRequest>()
            val user = login(loginRequest.username, hashPassword(loginRequest.password))
            if (user != null) {
                println("Login success $user")
                val token = JWT.create()
                    .withClaim("userId", user.id)
                    .withExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .sign(jwtAlgorithm)
                call.respond(LoginResponse(token, user.displayName ?: user.firstName))
            } else {
                call.respond(HttpStatusCode.Forbidden)
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
                        println(e.localizedMessage)
                    } finally {
                        println("removing connection")
                        connections -= thisConnection
                    }
                }
            }
        }
    }
}