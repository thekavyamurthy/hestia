import kotlinx.serialization.Serializable

@Serializable
data class Message(val fromId: Int, val fromName: String, val convId: Long, val body: String, val timestamp: Long)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val displayName: String, val tokenExpiry: Long)

@Serializable
data class Conversation(val name: String, val id: Long)