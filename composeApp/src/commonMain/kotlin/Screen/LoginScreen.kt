package Screen

import APIClient
import Screen.signupScreens.SignupScreen1
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import kotlinx.coroutines.launch

val LoginScreen by navDestination<Unit> {
    val navController = navController()

    var isVisible by remember { mutableStateOf(false) }


    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center
    ) {

        var email by remember { mutableStateOf("thekavyamurthy@gmail.com") }
        var password by remember { mutableStateOf("kavya") }
        val scope = rememberCoroutineScope()
//        val context = LocalContext.current;
//
//        val toast = Toast.makeText(context, "Login failed", Toast.LENGTH_LONG)

        //email
        Row {
            TextField(value = email, onValueChange = { email = it }, Modifier.padding(10.dp), placeholder = { Text("Enter your username") })
        }

        //password
        Row {
            TextField(value = password,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { password = it },
                modifier = Modifier.padding(10.dp),
                placeholder = { Text("Enter your password") })
        }

        Row {
            Button(modifier = Modifier.width(200.dp).padding(10.dp),
                onClick = {
                    scope.launch {
                        if (APIClient.login(email, password)) {
                            navController.replace(ConvScreen)
                        } else {
                            isVisible = true
                            println("login failed")
                        }
                    }
                }
            ) { Text(text = "Login") }
        }

        Row {
            if(isVisible) {
                Text(
                    text = "Login failed",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row {
            Text(
                text = buildAnnotatedString {
                    append("No account? Sign up ")
                    withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                        append("here")
                    }
                    append(".")
                },
                modifier = Modifier.clickable {
                    navController.navigate(SignupScreen1)
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

