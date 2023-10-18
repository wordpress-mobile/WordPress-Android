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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
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
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState.PopulatedList
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.UiState.Error

@Composable
fun MyDomainsScreen(uiState: UiState) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.domain_management_my_domains),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = {},
                actions = {
                    IconButton(
                        onClick = {},
                        enabled = uiState is PopulatedList.Loaded,
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
    ) {
        Column (
            modifier = Modifier.padding(it),
        ){
            val listState = rememberLazyListState()

            val elevation = animateDpAsState(
                targetValue = if (listState.canScrollBackward) 4.dp else 0.dp,
                label = "Search Input Elevation",
            )
            MyDomainsSearchInput(
                elevation.value,
                enabled = uiState is PopulatedList.Loaded,
            )
            when (uiState) {
                is PopulatedList ->
                    MyDomainsList(listUiState = uiState, listState = listState)
                is Error -> ErrorScreen()
                else -> {}
            }
        }
    }
}

@Composable
fun ErrorScreen() {
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = "Couldn't Update",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "Check that you're online and refresh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun MyDomainsSearchInput(
    elevation: Dp,
    enabled: Boolean = false,
) {
    var queryString by rememberSaveable { mutableStateOf("") }

    Surface (shadowElevation = elevation, modifier = Modifier.zIndex(1f)) {
        OutlinedTextField(
            value = queryString,
            onValueChange = { queryString = it },
            enabled = enabled,
            placeholder = { Text(stringResource(R.string.domain_management_search_your_domains)) },
            shape = RoundedCornerShape(50),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search_white_24dp),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.outline,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}


@Composable
fun MyDomainsList(
    listUiState: PopulatedList,
    listState: LazyListState,
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
            is PopulatedList.Loaded -> {
                items(items = listUiState.domains) {
                    DomainListCard(uiState = DomainCardUiState.fromDomain(domain = it))
                }
            }
        }
    }
}

@Preview(device = Devices.PIXEL_3A)
@Preview(device = Devices.PIXEL_3A, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewMyDomainsScreen() {
    M3Theme {
        MyDomainsScreen(PopulatedList.Initial)
    }
}
@Preview(device = Devices.PIXEL_3A)
@Preview(device = Devices.PIXEL_3A, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewMyDomainsScreenError() {
    M3Theme {
        MyDomainsScreen(Error)
    }
}
