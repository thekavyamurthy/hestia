import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object APIClient {
    private const val SERVER = "192.168.0.188:8080" //10.0.0.125:8080, 192.168.0.148:8080, 192.168.0.130:8080
    private const val WS_URL = "ws://$SERVER/chat"
    private const val API_URL = "http://$SERVER"

    private lateinit var authToken: String

    private val client = HttpClient(CIO) {

        // For Logging
        install(Logging) {
            level = LogLevel.ALL
        }

        // Timeout plugin
        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
            connectTimeoutMillis = 15000L
            socketTimeoutMillis = 15000L
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }

        install(ContentNegotiation) {
            json()
        }
    }

    private var wsConnection: DefaultClientWebSocketSession? = null
    //TODO: Is this the right way to do this?
    lateinit var user: String

    suspend fun getWebsocket(id: Int) =
        wsConnection ?: client.webSocketSession("$WS_URL/$id") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }.also { wsConnection = it }

    suspend fun login(username: String, password: String): Boolean {
        val resp = client.post("$API_URL/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        val loginResponse = resp.body<LoginResponse>()
        return if (resp.status == HttpStatusCode.OK) {
            authToken = loginResponse.token
            user = loginResponse.displayName
            true
        } else {
            authToken = ""
            false
        }
    }

    suspend fun listConversations(): ArrayList<Conversation> {
        val response = client.get("$API_URL/api/conversations") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
        return response.body()
    }

    suspend fun getConversation(id: Int): ArrayList<Message> {
        val response = client.get("$API_URL/api/conversation/$id") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
        return response.body()
    }
}