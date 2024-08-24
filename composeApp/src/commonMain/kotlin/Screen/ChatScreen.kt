package Screen

import APIClient
import Message
import MsgCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composegears.tiamat.navArgs
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ChatParams(val id: Long, val name: String)

val ChatScreen by navDestination<ChatParams> {
    val id = navArgs().id
    val name = navArgs().name
    val navController = navController()


    Column {
        val scope = rememberCoroutineScope()
        var msgInput by remember { mutableStateOf("") }
        val msgList = remember { mutableStateListOf<Message>() }

        LaunchedEffect(Unit) {
            val messages = APIClient.getConversation(id)
            messages.forEach { msgList.add(it) }
            println(msgList)
            try {
                while (true) {
                    val msg = APIClient.getWebsocket(id).receiveDeserialized<Message>()
                    msgList.add(msg)
                    println(msg)
                }
            } catch (_: ClosedReceiveChannelException){

            }
        }

        Row(Modifier.fillMaxWidth().background(color = Color(94, 54, 163)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = navController::back, modifier = Modifier.padding(10.dp)) {
                Text("Back")
            }
            Text(text = name, fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
        }

        Surface(Modifier.padding(5.dp)) {
            LazyColumn(userScrollEnabled = true, modifier = Modifier.fillMaxWidth().heightIn(max = 670.dp).padding(10.dp)) {
                items(msgList.size) { MsgCard(msgList[it]) }
            }
        }

        //send button
        Row {
            TextField(value = msgInput, onValueChange = { msgInput = it }, Modifier.padding(10.dp))

            Button(modifier = Modifier.width(200.dp).padding(10.dp),
                onClick = {
                    val msg = Message(0, APIClient.user, id, msgInput, Clock.System.now().epochSeconds)
                    msgList.add(msg)
                    scope.launch {
                        APIClient.getWebsocket(id).sendSerialized(msg)
                    }
                    msgInput = ""
                }
            ) { Text("Send") }
        }
    }
}