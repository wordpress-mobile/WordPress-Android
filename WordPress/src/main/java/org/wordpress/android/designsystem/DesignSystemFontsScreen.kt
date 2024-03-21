package org.wordpress.android.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
            FontCard(text = "Heading1", font = MaterialTheme.typography.heading1)
            FontCard(text = "Heading2", font = MaterialTheme.typography.heading2)
            FontCard(text = "Heading3", font = MaterialTheme.typography.heading3)
            FontCard(text = "Heading4", font = MaterialTheme.typography.heading4)

            FontsTitle("Body")
            FontCard(text = "Body Small", font = MaterialTheme.typography.bodySmall)
            FontCard(text = "Body Medium", font = MaterialTheme.typography.bodyMedium)
            FontCard(text = "Body Large", font = MaterialTheme.typography.bodyLarge)
            FontCard(text = "Body Small Emphasized", font = MaterialTheme.typography.bodySmallEmphasized)
            FontCard(text = "Body Medium Emphasized", font = MaterialTheme.typography.bodyMediumEmphasized)
            FontCard(text = "Body Large Emphasized", font = MaterialTheme.typography.bodyLargeEmphasized)

            FontsTitle("Miscellaneous")
            FontCard(text = "Footnote", font = MaterialTheme.typography.footnote)
            FontCard(text = "Footnote Emphasized", font = MaterialTheme.typography.footnoteEmphasized)
        }
    }
}
@Composable
fun FontCard (text: String, font: TextStyle) {
    Row (modifier = Modifier
        .padding(10.dp, 3.dp)
        .defaultMinSize(minHeight = 34.dp)
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
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

