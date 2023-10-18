package org.wordpress.android.ui.domains.management

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

@Composable
fun MyDomainsScreen(uiState: UiState) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "My Domains",
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "purchase a domain"
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
            MyDomainsSearchInput(elevation.value)
            when (uiState) {
                is PopulatedList ->
                    MyDomainsList(listUiState = uiState, listState = listState)
                else -> {}
            }
        }
    }
}

@Composable
fun MyDomainsSearchInput(elevation: Dp) {
    var queryString by rememberSaveable { mutableStateOf("") }

    Surface (shadowElevation = elevation, modifier = Modifier.zIndex(1f)) {
        OutlinedTextField(
            value = queryString,
            onValueChange = { queryString = it },
            placeholder = { Text("Search your domains") },
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
