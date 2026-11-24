package org.wordpress.android.ui.prefs.appsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard

private const val DISABLED_ALPHA = 0.38f

/**
 * An outlined card matching the Me screen's cards, with dividers between [rows] and an optional header above it.
 * Rows are passed as a list so conditional rows don't leave stray dividers behind.
 */
@Composable
internal fun SettingsCard(
    rows: List<@Composable () -> Unit>,
    title: String? = null,
) {
    val horizontalMargin = dimensionResource(R.dimen.content_margin)
    Column(modifier = Modifier.padding(horizontal = horizontalMargin, vertical = 8.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    .semantics { heading() }
            )
        }
        UnelevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    row()
                }
            }
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(16.dp)
    ) {
        RowTexts(title = title, summary = summary)
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RowTexts(title = title, summary = summary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * A caption followed by a link-style button, used for the policies in Privacy settings.
 */
@Composable
internal fun SettingsLearnMoreRow(
    caption: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
            Text(text = buttonText)
        }
    }
}

@Composable
private fun RowTexts(title: String, summary: String?) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (summary != null) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
