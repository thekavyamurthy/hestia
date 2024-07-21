package me.kavyamurthy.hestia

import io.ktor.websocket.DefaultWebSocketSession

class Connection(val session: DefaultWebSocketSession, val user: String) {
//    val store = ConversationStore(user)
}