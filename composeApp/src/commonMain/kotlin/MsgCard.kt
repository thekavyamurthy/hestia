
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

class CodeCard {

}

@Composable
fun FilterChipGroup(
    items: List<String>,
    defaultSelectedItemIndex: Int = 0,
    selectedItemIcon: ImageVector = Icons.Filled.Done,
    itemIcon: ImageVector = Icons.Filled.Build,
    onSelectedChanged: (Int) -> Unit = {}
) {
    var selectedItemIndex by remember { mutableStateOf(defaultSelectedItemIndex) }

    LazyRow(userScrollEnabled = true) {
        items(items.size) { index: Int ->
            FilterChip(
                modifier = Modifier.padding(end = 6.dp),
                selected = items[selectedItemIndex] == items[index],
                onClick = {
                    selectedItemIndex = index
                    onSelectedChanged(index)
                },
                label = { Text(items[index]) },
                leadingIcon = if (items[selectedItemIndex] == items[index]) {
                    {
                        Icon(
                            imageVector = selectedItemIcon,
                            contentDescription = "Localized Description",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = itemIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun MsgCard(msg: Message) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(all = 10.dp),
        text = "${msg.from}: ${msg.body}",
        fontFamily = FontFamily.Monospace
    )
}