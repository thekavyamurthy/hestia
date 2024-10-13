import kotlinx.serialization.Serializable

@Serializable
data class Message(val convType: ConversationType, val convId: Long, val timestamp: Long, val fromId: Int, val fromName: String, val body: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val displayName: String, val tokenExpiry: Long)

@Serializable
data class Conversation(val name: String, val id: Long, val type: ConversationType)

@Serializable
data class CreateUserReq(val userName: String, val displayName: String, val email: String, val password: String)

@Serializable
data class VerifyEmail(val email: String, val code: Int)

enum class ConversationType { DIRECT, SPACE }

@Serializable
data class Group(val id: Long, val name: String)

@Serializable
data class User(val id: Int, val userName: String, val displayName: String)