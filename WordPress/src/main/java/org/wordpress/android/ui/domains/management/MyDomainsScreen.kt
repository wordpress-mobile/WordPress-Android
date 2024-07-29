package org.wordpress.android.ui.domains.management

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState.Empty
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState.Error
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState.PopulatedList
import org.wordpress.android.ui.domains.management.composable.DomainsSearchTextField
import org.wordpress.android.ui.domains.management.composable.PrimaryButton
import org.wordpress.android.ui.themes.M3Theme

@Composable
fun MyDomainsScreen(
    uiState: UiState,
    onSearchQueryChanged: (String) -> Unit,
    onDomainTapped: (String, String) -> Unit,
    onAddDomainTapped: () -> Unit,
    onFindDomainTapped: () -> Unit,
    onBackTapped: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.domain_management_my_domains_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackTapped,
                actions = {
                    IconButton(
                        onClick = onAddDomainTapped,
                        enabled = uiState is PopulatedList.Loaded || uiState is Empty,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.domain_management_purchase_a_domain)
                        )
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            var queryString by rememberSaveable { mutableStateOf("") }
            val listState = rememberLazyListState()

            val elevation = animateDpAsState(
                targetValue = if (listState.canScrollBackward) 4.dp else 0.dp,
                label = "Search Input Elevation",
            )
            MyDomainsSearchInput(
                elevation.value,
                queryString = queryString,
                onQueryStringChanged = {
                    queryString = it
                    onSearchQueryChanged(it)
                },
                enabled = uiState is PopulatedList.Loaded,
            )
            when (uiState) {
                is PopulatedList -> MyDomainsList(
                    listUiState = uiState,
                    listState = listState,
                    onDomainTapped,
                )

                Error -> ErrorScreen(
                    titleRes = R.string.domain_management_error_title,
                    descriptionRes = R.string.domain_management_error_subtitle,
                    onRefresh = onRefresh
                )
                Empty -> EmptyScreen(onFindDomainTapped)
            }
        }
    }
}



@Composable
fun EmptyScreen(onFindDomainTapped: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.domain_management_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(R.string.domain_management_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        PrimaryButton(
            text = stringResource(R.string.domain_management_empty_find_domain),
            onClick = onFindDomainTapped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}


@Composable
fun MyDomainsSearchInput(
    elevation: Dp,
    queryString: String,
    onQueryStringChanged: (String) -> Unit,
    enabled: Boolean = false,
) {
    Surface(shadowElevation = elevation, modifier = Modifier.zIndex(1f)) {
        DomainsSearchTextField(
            value = queryString,
            onValueChange = onQueryStringChanged,
            enabled = enabled,
            placeholder = R.string.domain_management_search_your_domains,
        )
    }
}


@Composable
fun MyDomainsList(
    listUiState: PopulatedList,
    listState: LazyListState,
    onDomainTapped: (domain: String, detailUrl: String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp),
        state = listState,
    ) {
        when (listUiState) {
            PopulatedList.Initial ->
                repeat(2) {
                    item {
                        DomainListCard(uiState = DomainCardUiState.Initial)
                    }
                }

            is PopulatedList.Loaded.Complete -> {
                items(items = listUiState.allDomains) {
                    DomainListCard(uiState = DomainCardUiState.fromDomain(domain = it), onDomainTapped)
                }
            }

            is PopulatedList.Loaded.Filtered -> {
                items(items = listUiState.filtered) {
                    DomainListCard(uiState = DomainCardUiState.fromDomain(domain = it), onDomainTapped)
                }
            }
        }
    }
}

@Preview(device = Devices.PIXEL_3A, group = "Initial")
@Preview(device = Devices.PIXEL_3A, uiMode = UI_MODE_NIGHT_YES, group = "Initial")
@Composable
fun PreviewMyDomainsScreen() {
    M3Theme {
        MyDomainsScreen(
            uiState = PopulatedList.Initial,
            onSearchQueryChanged = {},
            onAddDomainTapped = {},
            onDomainTapped = { _, _ ->},
            onFindDomainTapped = {},
            onBackTapped = {},
        ) {}
    }
}

@Preview(device = Devices.PIXEL_3A, group = "Error / Offline")
@Preview(device = Devices.PIXEL_3A, uiMode = UI_MODE_NIGHT_YES, group = "Error / Offline")
@Composable
fun PreviewMyDomainsScreenError() {
    M3Theme {
        MyDomainsScreen(
            uiState = Error,
            onSearchQueryChanged = {},
            onAddDomainTapped = {},
            onDomainTapped = { _, _ ->},
            onFindDomainTapped = {},
            onBackTapped = {},
        ) {}
    }
}

@Preview(device = Devices.PIXEL_3A, group = "Empty")
@Preview(device = Devices.PIXEL_3A, uiMode = UI_MODE_NIGHT_YES, group = "Empty")
@Composable
fun PreviewMyDomainsScreenEmpty() {
    M3Theme {
        MyDomainsScreen(
            uiState = Empty,
            onSearchQueryChanged = {},
            onAddDomainTapped = {},
            onDomainTapped = { _, _ ->},
            onFindDomainTapped = {},
            onBackTapped = {},
        ) {}
    }
}
