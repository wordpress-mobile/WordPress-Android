package org.wordpress.android.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun DesignSystemColorsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.reader_follow_sheet_button_margin_top)
        )
    ) {
        item {
            ColorTitle("Foreground")
            val listForeground: List<ColorOption> = listOf(
                ColorOption("Primary", MaterialTheme.colorScheme.primary),
                ColorOption("Secondary", MaterialTheme.colorScheme.secondary),
                ColorOption("Tertiary", MaterialTheme.colorScheme.tertiary),
                ColorOption("Brand", MaterialTheme.colorScheme.brand),
                ColorOption("Error", MaterialTheme.colorScheme.error),
                ColorOption("Warning", MaterialTheme.colorScheme.warning),
                ColorOption("WP", MaterialTheme.colorScheme.wp),
                )
            ColorCardList(listForeground)

            ColorTitle("Background")
            val listBackground: List<ColorOption> = listOf(
                ColorOption("Primary", MaterialTheme.colorScheme.primaryContainer),
                ColorOption("Secondary", MaterialTheme.colorScheme.secondaryContainer),
                ColorOption("Tertiary", MaterialTheme.colorScheme.tertiaryContainer),
                ColorOption("Quaternary", MaterialTheme.colorScheme.quaternaryContainer),
                ColorOption("Brand", MaterialTheme.colorScheme.brandContainer),
                ColorOption("WP", MaterialTheme.colorScheme.wpContainer),
            )
            ColorCardList(listBackground)
        }
    }
}
@OptIn(ExperimentalStdlibApi::class)
@Composable
fun ColorCard (colorName: String, color: Color) {
    Row (modifier = Modifier.padding(10.dp, 3.dp).fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(shape = RoundedCornerShape(5.dp, 5.dp, 5.dp, 5.dp))
                    .background(color)
            )
        }
        Column {
            Text(
                modifier = Modifier.padding(start = 25.dp, end = 40.dp),
                text = colorName,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier.padding(start = 25.dp, end = 40.dp),
                text = "#" + color.value.toHexString().uppercase().substring(0,8),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
    Divider(modifier = Modifier.padding(start = 10.dp, end = 10.dp))
}
@Composable
fun ColorCardList(colorOptions: List<ColorOption>) {
    colorOptions.forEach { colorOption ->
        ColorCard(colorOption.title, colorOption.color)
    }
}
class ColorOption(var title: String, var color: Color)
@Composable
fun ColorTitle(title: String) {
    Text(
        modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
    )
}
@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun DesignSystemColorsScreenPreview() {
    DesignSystemTheme {
        DesignSystemColorsScreen()
    }
}

