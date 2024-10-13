package Screen

import APIClient
import Conversation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import kotlinx.coroutines.launch

val SpaceScreen by navDestination<Unit> {
    val navController = navController()

    val scope = rememberCoroutineScope()
    var spaceList by remember { mutableStateOf(ArrayList<Conversation>()) }


    LaunchedEffect(Unit) {
        spaceList = APIClient.listSpaces()
    }
    Scaffold(
        bottomBar = { BottomNavBar(navController, 2) }
    ) {
        Column {
            Surface(Modifier.padding(5.dp).weight(0.5F)) {
                LazyColumn(
                    userScrollEnabled = true,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp).padding(10.dp)
                ) {
                    items(spaceList.size) {
                        ConvCard(spaceList[it].type, spaceList[it].id, spaceList[it].name, navController)
                    }
                }
            }

            //get rid of this eventually please
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            APIClient.logOut()
                            navController.back()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    shape = RectangleShape
                ) {
                    Text("Logout")
                }
            }
        }
    }
}