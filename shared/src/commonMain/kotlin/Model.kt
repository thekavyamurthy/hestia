import kotlinx.serialization.*

@Serializable
data class Message(val from: String, val to: String, val body: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String)