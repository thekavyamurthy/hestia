import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hestia",
    ) {
        App(createDataStore { "" }) //TODO: Fix producer path
    }
}