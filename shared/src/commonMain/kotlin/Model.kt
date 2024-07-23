import kotlinx.serialization.Serializable

@Serializable
data class Message(val fromId: Int, val fromName: String, val toId: Int, val toName: String, val body: String, val timestamp: Long)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val displayName: String)

@Serializable
data class Conversation(val name: String, val id: Int)