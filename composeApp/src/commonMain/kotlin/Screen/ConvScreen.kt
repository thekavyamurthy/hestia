package Screen

import APIClient
import Conversation
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composegears.tiamat.NavController
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import hestia.composeapp.generated.resources.Res
import hestia.composeapp.generated.resources.gnomey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

val ConvScreen by navDestination<Unit> {
    val navController = navController()

    val scope = rememberCoroutineScope()
    var convList by remember { mutableStateOf(ArrayList<Conversation>()) }

    LaunchedEffect(Unit) {
        convList = APIClient.listConversations()
    }

    Column {

        //test if weight is working or not
        Surface(Modifier.padding(5.dp).weight(0.5F)) {
            LazyColumn(
                userScrollEnabled = true,
                modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp).padding(10.dp)
            ) {
                items(convList.size) { ConvCard(convList[it].id, convList[it].name, navController) }
            }
        }

        Row(Modifier.fillMaxWidth()) {
            Button(onClick = {
                scope.launch {
                    APIClient.logOut()
                    navController.back()
                }
            }, modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), shape = RectangleShape) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun ConvCard(id: Long, name: String, navController: NavController) {
    Row (Modifier.padding(20.dp).clickable { navController.navigate(ChatScreen, ChatParams(id, name)) }){
        Image(
            painter = painterResource(Res.drawable.gnomey),
            contentDescription = "gnomey",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .align(Alignment.CenterVertically)
        )
        Text(name,Modifier.padding(horizontal = 15.dp).align(Alignment.CenterVertically), fontSize = 20.sp)
    }
}