package Screen

import APIClient
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import kotlinx.coroutines.launch

val SignupScreen1 by navDestination<Unit> {
    val navController = navController()

    var email by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var enabled by rememberSaveable{ mutableStateOf(true)}
    var isVisible by remember { mutableStateOf(false) }


    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center
    ) {
        //email
        Row {
            TextField(
                value = email,
                onValueChange = { email = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Enter your email") })
        }

        Row {
            Button(onClick = {
                scope.launch {
                    if(APIClient.sendCode(email)) {
                        navController.navigate(SignupScreen2, email)
                    } else {
                        isVisible = true
                        println("Email in use already")
                    }
                }
            }) {
                Text("Next")
            }
        }

        Row {
            if(isVisible) {
                Text(
                    text = buildAnnotatedString {
                        append("Looks like you already have an account with us. Would you like to ")
                        withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                            append("log in ")
                        }
                        append("instead?")
                    },
                    modifier = Modifier.clickable(enabled = enabled) {
                        enabled = false
                        navController.back()
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}