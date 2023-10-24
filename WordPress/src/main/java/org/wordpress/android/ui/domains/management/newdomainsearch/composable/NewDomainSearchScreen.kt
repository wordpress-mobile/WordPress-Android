package org.wordpress.android.ui.domains.management.newdomainsearch.composable

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.domains.management.composable.DomainsSearchTextField

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
            Column (
                modifier = Modifier.padding(it),
            ) {
                val listState = rememberLazyListState()

                val elevation = animateDpAsState(
                    targetValue = if (listState.canScrollBackward) 4.dp else 0.dp,
                    label = "Search Input Elevation",
                )
                NewDomainSearchInput(
                    elevation.value
                )
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
