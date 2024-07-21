package me.kavyamurthy.hestia

import LoginRequest
import LoginResponse
import Message
import SERVER_PORT
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections

const val SECRET = "e42f5b73-f53d-428b-8fc0-98ac06e555db"


fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

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
            verifier(JWT.require(Algorithm.HMAC256(SECRET)).build())
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
            println(loginRequest)
            if (loginRequest.username == loginRequest.password) {
                val token = JWT.create()
                    .withClaim("username", loginRequest.username)
                    .withExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .sign(Algorithm.HMAC256(SECRET))
                call.respond(LoginResponse(token))
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        authenticate ("auth-jwt") {
            get("/api/conversations") {
                val principal = call.principal<JWTPrincipal>()
                val user = principal!!.payload.getClaim("username").asString()
                val convList = ConversationStore(user).listConversations()
                call.respond(convList)
            }

            get("/api/conversation/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val user = principal!!.payload.getClaim("username").asString()
                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "conversation id not specified")
                } else {
                    val conv = ConversationStore(user).getConversation(id)
                    call.respond(conv.getMessages())
                }
            }

            webSocket("/chat/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val user = principal!!.payload.getClaim("username").asString()
                val thisConnection = Connection(this, user)
                connections += thisConnection
                val id = call.parameters["id"]
                lateinit var conv: ConversationStore.Conversation

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "conversation id not specified")
                } else {
                    conv = ConversationStore(user).getConversation(id)
                }

                try {
                    while(true) {
                        val msg = receiveDeserialized<Message>()
                        connections.forEach {
                            if (msg.to == it.user)
                                (it.session as WebSocketServerSession).sendSerialized(msg)
                            conv.addMessage(msg)
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