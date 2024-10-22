package org.wordpress.android.ui.domains.management.purchasedomain.composable

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.domains.management.composable.PrimaryButton
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.UiState
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainViewModel.UiState.Initial
import org.wordpress.android.ui.compose.theme.success

@Composable
fun PurchaseDomainScreen(
    uiState: UiState,
    onNewDomainCardSelected: () -> Unit,
    onExistingSiteCardSelected: () -> Unit,
    onErrorButtonTapped: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentScrollState = rememberScrollState()
    Scaffold(
        modifier = modifier,
        topBar = {
            val elevation = animateDpAsState(
                targetValue = if (contentScrollState.value == 0) 0.dp else 4.dp,
                label = "AppBarElevation"
            )
            MainTopAppBar(
                title = stringResource(id = R.string.purchase_domain_screen_title),
                navigationIcon = NavigationIcons.BackIcon,
                elevation = elevation.value,
                onNavigationIconClick = onBackPressed,
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        content = {
            if (uiState == UiState.ErrorSubmittingCart || uiState == UiState.ErrorInCheckout) {
                ErrorScreen(onButtonTapped = onErrorButtonTapped)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(it)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(contentScrollState)
                            .padding(16.dp)
                    ) {
                        ScreenHeader()
                        ScreenDescription()
                        DomainCards(uiState, onNewDomainCardSelected, onExistingSiteCardSelected)
                        DiscountNotice()
                    }
                }
            }
        }
    )
}

@Composable
private fun DomainCards(
    uiState: UiState,
    onNewDomainCardSelected: () -> Unit,
    onExistingSiteCardSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(top = 24.dp)) {
        val arrangement = Arrangement.spacedBy(16.dp)
        if (isPortrait) {
            Column(verticalArrangement = arrangement) {
                NewDomainCard(uiState, onNewDomainCardSelected)
                ExistingSiteCard(uiState, onExistingSiteCardSelected)
            }
        } else {
            Row(horizontalArrangement = arrangement) {
                NewDomainCard(uiState, onNewDomainCardSelected, modifier = Modifier.weight(1f))
                ExistingSiteCard(uiState, onExistingSiteCardSelected, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScreenHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            id = if (isPortrait) {
                R.string.purchase_domain_screen_header_double_line
            } else {
                R.string.purchase_domain_screen_header_single_line
            }
        ),
        style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier
    )
}

@Composable
private fun ScreenDescription(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.purchase_domain_screen_description),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = modifier.padding(top = 8.dp)
    )
}

@Composable
private fun DiscountNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.purchase_domain_screen_discount_notice),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = modifier.padding(top = 16.dp),
    )
}

@Composable
private fun NewDomainCard(
    uiState: UiState,
    onNewDomainCardSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainOptionCard(
        icon = R.drawable.ic_domains_white_24dp,
        title = R.string.purchase_domain_screen_new_domain_card_title,
        description = R.string.purchase_domain_screen_new_domain_card_description,
        button = R.string.purchase_domain_screen_new_domain_card_button,
        isEnabled = uiState == Initial,
        isInProgress = uiState == UiState.SubmittingJustDomainCart,
        onOptionSelected = onNewDomainCardSelected,
        modifier = modifier,
    )
}

@Composable
private fun ExistingSiteCard(
    uiState: UiState,
    onExistingSiteCardSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainOptionCard(
        icon = R.drawable.ic_themes_white_24dp,
        title = R.string.purchase_domain_screen_existing_domain_card_title,
        description = R.string.purchase_domain_screen_existing_domain_card_description,
        button = R.string.purchase_domain_screen_existing_domain_card_button,
        isEnabled = uiState == Initial,
        isInProgress = uiState == UiState.SubmittingSiteDomainCart,
        onOptionSelected = onExistingSiteCardSelected,
        modifier = modifier,
    )
}

@Composable
private fun DomainOptionCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes button: Int,
    isEnabled: Boolean,
    isInProgress: Boolean,
    onOptionSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                width = 0.5.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = decorativeIconContentDescription,
            tint = MaterialTheme.colorScheme.success,
            modifier = Modifier.size(36.dp)
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(id = title),
            style = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = stringResource(id = description),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        PrimaryButton(
            isEnabled = isEnabled,
            isInProgress = isInProgress,
            onClick = onOptionSelected,
            text = stringResource(button),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun ErrorScreen(onButtonTapped: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Text(
            text = stringResource(R.string.purchase_domain_screen_error_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(R.string.purchase_domain_screen_error_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        PrimaryButton(
            text = stringResource(R.string.purchase_domain_screen_error_button_title),
            onClick = onButtonTapped,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}

private val isPortrait: Boolean @Composable get() = LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT

@Preview(name = "Light mode", locale = "en")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen", device = Devices.NEXUS_5)
@Preview(name = "Landscape orientation", device = Devices.AUTOMOTIVE_1024p)
@Composable
fun PurchaseDomainScreenPreview() {
    AppThemeM3 {
        PurchaseDomainScreen(Initial, {}, {}, {}, {})
    }
}

@Preview(name = "Light mode", locale = "en")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PurchaseDomainScreenErrorPreview() {
    AppThemeM3 {
        PurchaseDomainScreen(UiState.ErrorSubmittingCart, {}, {}, {}, {})
    }
}

private val decorativeIconContentDescription: String? = null
