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
import com.composegears.tiamat.navArgs
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import kotlinx.coroutines.launch

val SignupScreen2 by navDestination<String> {
    val scope = rememberCoroutineScope()
    val navController = navController()
    val email = navArgs()
    var code by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }


    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            TextField(
                value = code,
                onValueChange = { code = it },
                Modifier.padding(10.dp),
                placeholder = { Text("Enter the verification code sent to your email") })
        }

        Row {
            Button(onClick = {
                scope.launch {
                    if(APIClient.verifyCode(email, code.toInt())) {
                        navController.navigate(SignupScreen3)
                    } else {
                        isVisible = true
                    }
                }
            }) {
                Text("Next")
            }
        }

        Row {
            if(isVisible) {
                Text(
                    text = "Incorrect code.",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row {
            Text(
                text = buildAnnotatedString {
                    append("Didn't receive a code? ")
                    withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                        append("Send another")
                    }
                    append(".")
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        APIClient.sendCode(email)
                    }
                },
                textAlign = TextAlign.Center
            )
        }
    }
}