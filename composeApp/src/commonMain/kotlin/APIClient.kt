
import Screen.Prefs
import Screen.emailKey
import Screen.tokenExpiryKey
import Screen.tokenKey
import Screen.userKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

object APIClient {
    private const val SERVER = "hestia.fun"
//    private const val SERVER = "192.168.0.188:8080"
    private const val WS_URL = "ws://$SERVER/chat"
    private const val API_URL = "http://$SERVER"

    private lateinit var dataStore: DataStore<Preferences>

    private lateinit var authToken: String
    private lateinit var tokenExpiry: Instant

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
    lateinit var email: String


    suspend fun getWebsocket(id: Long) =
        wsConnection ?: client.webSocketSession("$WS_URL/$id") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }.also { wsConnection = it }

    fun getDataStore(store: DataStore<Preferences>) {
        dataStore = store
    }

    fun getPreferences(prefs: Prefs) {
        authToken = prefs.token
        user = prefs.user
        email = prefs.email
        tokenExpiry = Instant.fromEpochSeconds(prefs.tokenExpiry)
    }

    suspend fun setPreferences() {
        dataStore.edit {
            it[tokenKey] = authToken
            it[emailKey] = email
            it[userKey] = user
            it[tokenExpiryKey] = tokenExpiry.epochSeconds
        }
    }

    suspend fun login(emailId: String, password: String): Boolean {
        val resp = client.post("$API_URL/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(emailId, password))
        }
        return if (resp.status == HttpStatusCode.OK) {
            val loginResponse = resp.body<LoginResponse>()
            email = emailId
            authToken = loginResponse.token
            user = loginResponse.displayName
            tokenExpiry = Instant.fromEpochSeconds(loginResponse.tokenExpiry)
            setPreferences()
            true
        } else {
            authToken = ""
            println(resp.body<Any?>().toString())
            false
        }
    }

    //make display name optional
    suspend fun signup(userName: String, displayName: String, emailId: String, password: String): Boolean {
        val response = client.post("$API_URL/api/signup") {
            contentType(ContentType.Application.Json)
            setBody(CreateUserReq(userName, displayName, emailId, password))
        }
        if (response.status == HttpStatusCode.OK) {
            println("USER CREATED! $response")
            return (login(emailId, password))
        } else {
            println(response.status)
            return false
        }
    }

    suspend fun sendCode(email: String): Boolean {
        val response = client.post("$API_URL/api/sendCode") {
            setBody(email)
        }
        if (response.status == HttpStatusCode.OK) {
            println("Code sent!")
            return true
        } else if (response.status == HttpStatusCode.Conflict){
            println(response.status)
            return false
        } else {
            println(response.status)
            throw Exception("Unexpected Server Error")
        }
    }

    suspend fun verifyCode(emailId: String, code: Int): Boolean {
        val response = client.post("$API_URL/api/verifyCode") {
            contentType(ContentType.Application.Json)
            setBody(VerifyEmail(emailId, code))
        }
        when (response.status) {
            HttpStatusCode.OK -> {
                println("Email Verified!")
                authToken = response.body<String>()
                email = emailId;
                return true
            }
            HttpStatusCode.Forbidden -> {
                println(response.status)
                return false
            }
            else -> {
                println(response.status)
                throw Exception("Unexpected Server Error")
            }
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

    suspend fun getConversation(id: Long): ArrayList<Message> {
        val response = client.get("$API_URL/api/conversation/$id") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
        return response.body()
    }

    suspend fun logOut(): Unit {
        dataStore.edit {
            it.clear()
        }
    }
}