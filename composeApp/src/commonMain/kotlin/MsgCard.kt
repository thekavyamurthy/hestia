
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun MsgCard(msg: Message) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(all = 10.dp),
        text = "${msg.from}: ${msg.body}",
        fontFamily = FontFamily.Monospace
    )
}