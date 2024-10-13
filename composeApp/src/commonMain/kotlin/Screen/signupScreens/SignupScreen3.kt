package Screen.signupScreens

import APIClient
import Screen.ConvScreen
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import kotlinx.coroutines.launch

val SignupScreen3 by navDestination<Unit> {
    val navController = navController()
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwdError by remember { mutableStateOf(false) }
    var userNameError by remember { mutableStateOf(false) }


    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center
    ) {
        //userName
        Row {
            TextField(
                value = userName,
                onValueChange = { userName = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Enter your user name") })
        }

        //displayName
        Row {
            TextField(
                value = displayName,
                onValueChange = { displayName = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Enter your display name") })
        }

        //password
        Row {
            TextField(
                value = password,
                onValueChange = { password = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Enter your password") })
        }

        //password confirm
        Row {
            TextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Re-enter your password") })
        }

        Row {
            Button(onClick = {
                scope.launch {
                    if (password == passwordConfirm) {
                        passwdError = false
                        if(APIClient.signup(userName, displayName, APIClient.email, password)) {
                            userNameError = false
                            navController.replace(ConvScreen)
                        } else {
                            userNameError = true
                        }
                    } else {
                        passwdError = true
                    }
                }
            }) {
                Text("Next")
            }
        }

        Row {
            if(passwdError) {
                Text(
                    text = "Password and confirmed password fields do not match.",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row {
            if(userNameError) {
                Text(
                    text = "This username is already in use. Please pick another.",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }

        //SEE IF THIS IS WORKING
        Row {
            if(userName.contains(" ")) {
                Text(
                    text = "Your username cannot contain any spaces.",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
