package org.wordpress.android.ui.domains.management.newdomainsearch.composable

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.domains.management.composable.DomainsSearchTextField
import org.wordpress.android.ui.domains.management.success

@Composable
fun NewDomainSearchScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopAppBar(
                title = stringResource(id = R.string.new_domain_search_screen_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackPressed,
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        content = {
            Column {
                Column(
                    modifier = Modifier.padding(it),
                ) {
                    val listState = rememberLazyListState()

                    val elevation = animateDpAsState(
                        targetValue = if (listState.canScrollBackward) 4.dp else 0.dp,
                        label = "Search Input Elevation",
                    )
                    NewDomainSearchInput(elevation = elevation.value)
                }
                Spacer(modifier = Modifier.weight(1f))
                TransferDomainFooter()
            }
        }
    )
}

@Composable
fun NewDomainSearchInput(
    elevation: Dp,
    modifier: Modifier = Modifier
) {
    var queryString by rememberSaveable { mutableStateOf("") }

    Surface (shadowElevation = elevation, modifier = Modifier.zIndex(1f)) {
        DomainsSearchTextField(
            value = queryString,
            enabled = true,
            placeholder = R.string.new_domain_search_screen_input_placeholder,
            modifier = modifier,
            onValueChange = { queryString = it }
        )
    }
}

@Composable
fun TransferDomainFooter(
    modifier: Modifier = Modifier
) {
    Surface (shadowElevation = 4.dp, modifier = Modifier.zIndex(1f)) {
        Column(
            modifier = modifier
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.new_domain_search_screen_transfer_domain_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {},
                shape = MaterialTheme.shapes.small.copy(CornerSize(36.dp)),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.new_domain_search_screen_transfer_domain_button),
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.success,
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(name = "Light mode", locale = "en")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen", device = Devices.NEXUS_5)
@Preview(name = "Landscape orientation", device = Devices.AUTOMOTIVE_1024p)
@Composable
fun NewDomainSearchScreenPreview() {
    NewDomainSearchScreen({})
}
