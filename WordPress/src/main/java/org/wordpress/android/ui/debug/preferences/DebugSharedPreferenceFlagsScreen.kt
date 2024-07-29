package org.wordpress.android.ui.debug.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.M3Theme

@Composable
fun DebugSharedPreferenceFlagsScreen(
    flags: Map<String, Boolean>,
    onBackTapped: () -> Unit,
    onFlagChanged: (String, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.debug_settings_debug_flags_screen),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackTapped,
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(flags.toList()) { (key, value) ->
                DebugFlagRow(
                    key = key,
                    value = value,
                    onFlagChanged = onFlagChanged,
                )
            }
        }
    }
}

@Composable
fun DebugFlagRow(
    key: String,
    value: Boolean,
    onFlagChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onFlagChanged(key, !value) }
    ) {
        Text(
            text = key,
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically)
                .padding(start = 16.dp)
        )
        Checkbox(
            checked = value,
            modifier = Modifier.align(CenterVertically).padding(end = 8.dp),
            onCheckedChange = { onFlagChanged(key, it) }
        )
    }
}

@Preview
@Composable
fun DebugFlagsScreenPreview() {
    M3Theme {
        DebugSharedPreferenceFlagsScreen(
            flags = mapOf(
                "EXAMPLE_FEATURE_FLAG" to true,
                "RANDOM_FLAG" to false,
                "ANOTHER_FLAG" to true,
                "YET_ANOTHER_FLAG" to false,

            ),
            onBackTapped = {},
            onFlagChanged = { _, _ -> },
        )
    }
}
