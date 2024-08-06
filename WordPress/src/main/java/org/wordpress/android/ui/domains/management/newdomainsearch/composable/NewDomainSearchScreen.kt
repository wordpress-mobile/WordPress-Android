package org.wordpress.android.ui.domains.management.newdomainsearch.composable

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.domains.management.ErrorScreen
import org.wordpress.android.ui.domains.management.composable.DomainsSearchTextField
import org.wordpress.android.ui.domains.management.composable.PendingGhostStrip
import org.wordpress.android.ui.domains.management.newdomainsearch.NewDomainSearchViewModel.UiState
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.ProposedDomain
import org.wordpress.android.ui.compose.theme.success

@Composable
fun NewDomainSearchScreen(
    uiState: UiState,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onTransferDomainClicked: () -> Unit,
    onDomainTapped: (domain: ProposedDomain) -> Unit,
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
            Column(
                modifier = Modifier.padding(it),
            ) {
                val listState = rememberLazyListState()

                val elevation = animateDpAsState(
                    targetValue = if (listState.canScrollBackward) 4.dp else 0.dp,
                    label = "Search Input Elevation",
                )
                NewDomainSearchInput(elevation = elevation.value, onValueChanged = onSearchQueryChanged)
                when (uiState) {
                    is UiState.PopulatedDomains -> ProposedDomainList(
                        domains = uiState.domains,
                        listState = listState,
                        onDomainTapped = onDomainTapped,
                        modifier = Modifier.weight(1f)
                    )
                    is UiState.Loading -> LoadingPlaceholder(modifier = Modifier.weight(1f))
                    is UiState.Error -> ErrorScreen(
                        titleRes = R.string.new_domain_search_screen_error_title,
                        descriptionRes = R.string.domain_management_error_subtitle,
                        onRefresh = onRefresh,
                        modifier = Modifier.weight(1f)
                    )
                }
                TransferDomainFooter(onTransferDomainClicked = onTransferDomainClicked)
            }
        }
    )
}

@Composable
fun NewDomainSearchInput(
    elevation: Dp,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var queryString by rememberSaveable { mutableStateOf("") }

    Surface(shadowElevation = elevation, modifier = Modifier.zIndex(1f)) {
        DomainsSearchTextField(
            value = queryString,
            enabled = true,
            placeholder = R.string.new_domain_search_screen_input_placeholder,
            modifier = modifier,
            onValueChange = {
                queryString = it
                onValueChanged(it)
            }
        )
    }
}

@Composable
fun ProposedDomainList(
    domains: List<ProposedDomain>,
    listState: LazyListState,
    onDomainTapped: (domain: ProposedDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        items(items = domains) { domain -> Domain(domain = domain, onDomainTapped = onDomainTapped) }
    }
}

@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(times = 2) {
            Column(modifier = Modifier.padding(16.dp)) {
                PendingGhostStrip(width = 100.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    PendingGhostStrip(width = 150.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    PendingGhostStrip(width = 80.dp)
                }
            }
        }
    }
}

@Composable
fun TransferDomainFooter(
    onTransferDomainClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(shadowElevation = 8.dp, modifier = Modifier.zIndex(1f)) {
        Column(
            modifier = modifier
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.new_domain_search_screen_transfer_domain_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            JetpackOutlinedButton(
                text = stringResource(R.string.new_domain_search_screen_transfer_domain_button),
                onClick = onTransferDomainClicked
            )
        }
    }
}

@Composable
fun Domain(
    domain: ProposedDomain,
    onDomainTapped: (domain: ProposedDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onDomainTapped(domain) },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(
                    text = domain.domainPrefix,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = domain.domainSuffix,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (domain.salePrice.isNullOrEmpty()) {
                Text(
                    text = stringResource(
                        id = R.string.new_domain_search_screen_list_item_regular_price,
                        domain.price
                    )
                )
            } else {
                Row {
                    Text(
                        text = stringResource(
                            id = R.string.new_domain_search_screen_list_item_sale_price, domain.salePrice
                        ),
                        modifier = Modifier.padding(end = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.success
                    )
                    Text(
                        text = stringResource(
                            id = R.string.new_domain_search_screen_list_item_regular_price,
                            domain.price
                        ),
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
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
    NewDomainSearchScreen(
        uiState = UiState.PopulatedDomains(
            domains = listOf(
                ProposedDomain(
                    productId = 0,
                    domain = "example.com",
                    price = "USD 100",
                    salePrice = "USD 30",
                    supportsPrivacy = true,
                ),
                ProposedDomain(
                    productId = 0,
                    domain = "example.blog",
                    price = "USD 100",
                    salePrice = null,
                    supportsPrivacy = true,
                ),
            )
        ),
        onSearchQueryChanged = {},
        onRefresh = {},
        onTransferDomainClicked = {},
        onDomainTapped = {},
        onBackPressed = {}
    )
}
