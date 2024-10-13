package me.kavyamurthy.hestia

import io.ktor.websocket.WebSocketSession

class Connection(val session: WebSocketSession, val userId: Int)