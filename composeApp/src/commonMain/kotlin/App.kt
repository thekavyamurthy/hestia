

import Screen.AddMemberScreen
import Screen.ChatScreen
import Screen.ConvScreen
import Screen.GroupDetailScreen
import Screen.GroupScreen
import Screen.LoadingScreen
import Screen.LoginScreen
import Screen.NewConvScreen
import Screen.SpaceScreen
import Screen.signupScreens.SignupScreen1
import Screen.signupScreens.SignupScreen2
import Screen.signupScreens.SignupScreen3
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
            SignupScreen1,
            SignupScreen2,
            SignupScreen3,
            ConvScreen,
            NewConvScreen,
            ChatScreen,
            SpaceScreen,
            GroupScreen,
            GroupDetailScreen,
            AddMemberScreen
        ),
        startDestinationNavArgs = dataStore
    )


    Navigation(
        modifier = Modifier.fillMaxSize(),
        navController = navController
    )
}