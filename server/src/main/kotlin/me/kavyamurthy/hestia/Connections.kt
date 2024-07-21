package me.kavyamurthy.hestia

import io.ktor.websocket.*
import java.util.concurrent.atomic.*

private val lastId = AtomicInteger(0)

class Connection(val session: DefaultWebSocketSession, val user: String) {
    val store = ConversationStore(user)
}