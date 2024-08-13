

import Screen.ChatScreen
import Screen.ConvScreen
import Screen.LoadingScreen
import Screen.LoginScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.composegears.tiamat.Navigation
import com.composegears.tiamat.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App(dataStore: DataStore<Preferences>) {

    val navController = rememberNavController(
        startDestination = LoadingScreen,
        destinations = arrayOf(
            LoadingScreen,
            LoginScreen,
            ConvScreen,
            ChatScreen
        ),
        startDestinationNavArgs = dataStore
    )


    Navigation(
        modifier = Modifier.fillMaxSize(),
        navController = navController
    )
}