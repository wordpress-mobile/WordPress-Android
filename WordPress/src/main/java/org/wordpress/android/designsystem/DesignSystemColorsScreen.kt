package org.wordpress.android.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
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
import org.wordpress.android.designsystem.DesignSystemDataSource.list

@Composable
fun DesignSystemColorsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.reader_follow_sheet_button_margin_top)
        )
    ) {
        item {
            ColorCardList(list)
        }
    }
}
@Composable
fun ColorCard (colorName: String, colorHex: String) {
    Row (modifier = Modifier.padding(all = 8.dp)) {
        Box (Modifier.background(DesignSystemTheme.colors.tertiaryBackground)) {
            Column(
                modifier = Modifier
                    .padding(30.dp)
                    .then(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.TopCenter)
                    )
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 36.dp, end = 64.dp),
                    text = colorName,
                    color = DesignSystemTheme.colors.primaryForeground,
                )
                Text(
                    modifier = Modifier.padding(start = 36.dp, end = 64.dp),
                    text = colorHex,
                    color = DesignSystemTheme.colors.primaryForeground
                )
            }
            Column(
                modifier = Modifier
                    .padding(15.dp)
                    .then(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.TopStart)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CardDefaults.shape)
                        .background(Color.Red)
                )
            }
        }
    }
}
@Composable
fun ColorCardList(colorOptions: List<ColorOption>) {
    colorOptions.forEach { colorOption ->
        ColorCard(colorOption.title, colorOption.hex)
    }
}
    class ColorOption(var title: String, var hex: String) {
        fun getInfo(): String {
            return "$title $hex"
        }
    }

@Preview
@Composable
fun DesignSystemColorsScreenPreview() {
    val list: List<ColorOption> = listOf(
        ColorOption("Primary Foreground", "#000000"),
        ColorOption("Primary Background", "#FFFFFF")
    )

    DesignSystemTheme {
        ColorCardList(list)
    }
}

