

import Screen.ChatScreen
import Screen.ConvScreen
import Screen.LoginScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composegears.tiamat.Navigation
import com.composegears.tiamat.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {

    val navController = rememberNavController(
        startDestination = LoginScreen,
        destinations = arrayOf(
            LoginScreen,
            ConvScreen,
            ChatScreen
        )
    )

    Navigation(
        modifier = Modifier.fillMaxSize(),
        navController = navController
    )
}