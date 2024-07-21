package me.kavyamurthy.hestia

import Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream

private val baseDir = File("/Users/kavyamurthy/hestia")

class ConversationStore(user: String) {
    private val userDir = File(baseDir, user)

    init {
        userDir.mkdirs()
    }

    fun getConversation(id: String) = Conversation(id)

    fun listConversations(): ArrayList<String> {
        val convArray = userDir.listFiles()
        val convList = ArrayList<String>()
        if(convArray != null) {
            repeat(convArray.size) { i ->
                if (convArray[i].name.endsWith(".conv")) {
                    convList.add(convArray[i].name.dropLast(5))
                }
            }
            println(convList)
        }
        return convList
    }

    inner class Conversation(name: String) {
        private val convFile = File(userDir, "$name.conv")
        private val messages = ArrayList<Message>()
        private val writer: BufferedWriter

        init {
            convFile.inputStream().bufferedReader().forEachLine { line ->
                messages.add(Json.decodeFromString(line))
            }
            writer = FileOutputStream(convFile, true).bufferedWriter()
        }

        fun addMessage(message: Message) {
            messages.add(message)
            writer.write(Json.encodeToString(message))
            writer.write("\n")
            writer.flush()
        }

        fun getMessages(): List<Message> = messages
    }

}

