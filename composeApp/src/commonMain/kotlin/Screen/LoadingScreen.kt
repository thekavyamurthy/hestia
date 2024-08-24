package Screen

import APIClient
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.composegears.tiamat.navArgs
import com.composegears.tiamat.navController
import com.composegears.tiamat.navDestination
import kotlinx.datetime.Clock

val tokenKey = stringPreferencesKey("token")
val emailKey = stringPreferencesKey("email")
val userKey = stringPreferencesKey("user")
val tokenExpiryKey = longPreferencesKey("tokenExpiry")

//TODO: Name this better
data class Prefs(val token: String, val email: String, val user: String, val tokenExpiry: Long)

val LoadingScreen by navDestination<DataStore<Preferences>> {
    val dataStore = navArgs()
    val navController = navController()

    LaunchedEffect(Unit) {
        println("launched effect")
        APIClient.getDataStore(dataStore)
        dataStore.edit {
            println("data edit")
            if (it[tokenKey] == null || it[tokenKey] == "") {
                println("no token")
                navController.navigate(LoginScreen)
            } else {
                println("token")
                //test this
                if((it[tokenExpiryKey] ?: Clock.System.now().epochSeconds) > Clock.System.now().epochSeconds) {
                    println("Token not expired")
                    APIClient.getPreferences(
                        Prefs(
                            it[tokenKey] ?: "",
                            it[emailKey] ?: "",
                            it[userKey] ?: "",
                            it[tokenExpiryKey] ?: 0
                        )
                    )
                    navController.navigate(ConvScreen)
                } else {
                    println("Token expired")
                    navController.navigate(LoginScreen)
                }
                println("prefs set")
            }
        }
    }

    Text("Loading...")
}