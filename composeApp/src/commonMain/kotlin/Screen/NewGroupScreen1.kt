package Screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination

val NewGroupScreen1 by navDestination<Unit> {
    val navController = navController()

    var name by remember { mutableStateOf("") }
    var users by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var enabled by rememberSaveable{ mutableStateOf(true) }
    var isVisible by remember { mutableStateOf(false) }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center
    ) {
        //name
        Row {
            TextField(
                value = name,
                onValueChange = { name = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Enter a name for your group") })
        }

        Row {
            Button(onClick = {
//                scope.launch {
//                    val group = APIClient.newGroup(name)
//                    if (group.id != 0L) {
//                        navController.replace(GroupScreen, Group(group.id, group.name))
//                    } else {
//                        isVisible = true
//                    }
//                }
            }) {
                Text("Start chat")
            }
        }

        Row {
            if(isVisible) {
                Text(
                    text = "Looks like an account with this username/email doesn't exist.",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(enabled = enabled) {
                        enabled = false
                        navController.back()
                    }
                )
            }
        }
    }
}