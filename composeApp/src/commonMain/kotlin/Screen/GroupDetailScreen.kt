package Screen

import APIClient
import Group
import User
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.composegears.tiamat.navArgs
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import hestia.composeapp.generated.resources.Res
import hestia.composeapp.generated.resources.gnomey
import org.jetbrains.compose.resources.painterResource

val GroupDetailScreen by navDestination<Group> {
    val id = navArgs().id
    val name = navArgs().name
    val navController = navController()

    var members by remember { mutableStateOf(ArrayList<User>()) }

    LaunchedEffect(Unit) {
        members = APIClient.listMembers(id)
    }

    Column {
        Text(name)
        Surface(Modifier.padding(5.dp).weight(0.5F)) {
            LazyColumn(
                userScrollEnabled = true,
                modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp).padding(10.dp)
            ) {
                items(members.size) {
                    Row (Modifier.padding(20.dp).clickable { /* nothing rn */ }){
                        Image(
                            painter = painterResource(Res.drawable.gnomey),
                            contentDescription = "gnomey",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .align(Alignment.CenterVertically)
                        )
                        Text(members[it].displayName, Modifier.padding(horizontal = 15.dp).align(Alignment.CenterVertically), fontSize = 20.sp)
                    }
                }
                item {
                    Row (Modifier.padding(20.dp).clickable { navController.navigate(AddMemberScreen, Group(id, name)) }) {
                        Image(
                            painter = painterResource(Res.drawable.gnomey),
                            contentDescription = "gnomey",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .align(Alignment.CenterVertically)
                        )
                        Text("Add Member", Modifier.padding(horizontal = 15.dp).align(Alignment.CenterVertically), fontSize = 20.sp)
                    }
                }
            }
        }
    }
}