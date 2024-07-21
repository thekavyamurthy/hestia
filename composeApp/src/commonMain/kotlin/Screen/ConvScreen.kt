package Screen

import APIClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composegears.tiamat.NavController
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import gnomey
import org.jetbrains.compose.resources.painterResource

val ConvScreen by navDestination<Unit> {
    val navController = navController()

    var convList by remember { mutableStateOf(ArrayList<String>()) }

    LaunchedEffect(Unit) {
        convList = APIClient.listConversations()
    }

    Column {
        Spacer(Modifier.height(10.dp))
        convList.forEach {
            ConvCard(it, navController)
        }
    }
}

@Composable
fun ConvCard(name: String, navController: NavController) {
    Row (Modifier.padding(20.dp).clickable { navController.replace(ChatScreen, ChatParams(name)) }){
        Image(
            painter = painterResource(gnomey),
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