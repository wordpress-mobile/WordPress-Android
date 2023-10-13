package org.wordpress.android.ui.domains.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import java.time.Instant
import java.time.LocalDate

@Composable
fun MyDomainsScreen(uiState: DomainManagementViewModel.UiState) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "My Domains",
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = {},
            )
        },
    ) {
        Column (
            modifier = Modifier.padding(it),
        ){
            MyDomainsSearchInput()
            when (uiState) {
                is DomainManagementViewModel.UiState.Populated ->
                    MyDomainsList(uiState.domains)
                else -> {}
            }
        }
    }
}

@Composable
fun MyDomainsSearchInput() {
    var queryString by rememberSaveable { mutableStateOf("") }

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


@Composable
fun MyDomainsList(domains: List<AllDomainsDomain>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        items(items = domains) {
            DomainListCard(
                uiState = DomainCardUiState(
                    domain = it.domain,
                    title = null,
                    status = it.domainStatus,
                    expiry = it.expiry?.time?.let { epoch ->
                        LocalDate.from(Instant.ofEpochMilli(epoch))
                    },
                )
            )
        }
    }
}

@Preview
@Composable
fun PreviewMyDomainsScreen() {
    M3Theme {
        MyDomainsScreen(DomainManagementViewModel.UiState.Initial)
    }
}
