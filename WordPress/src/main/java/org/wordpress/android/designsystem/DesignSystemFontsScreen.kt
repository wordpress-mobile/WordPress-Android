package org.wordpress.android.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun DesignSystemFontsScreen(
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
            FontsTitle("Heading")
@Composable
fun FontCard (text: String, font: TextStyle) {
    Row (modifier = Modifier
        .padding(10.dp, 3.dp)
        .fillMaxWidth()) {
        Column {
            Text(
                modifier = Modifier.padding(start = 25.dp, end = 40.dp),
                style = font,
                text = text,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Divider(modifier = Modifier.padding(start = 10.dp, end = 10.dp))
}
@Composable
fun FontsTitle(title: String) {
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
fun DesignSystemFontsScreenPreview() {
    DesignSystemTheme {
        DesignSystemFontsScreen()
    }
}

