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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.domains.management.success

@Composable
fun PurchaseDomainScreen(
    onNewDomainCardSelected: () -> Unit,
    onExistingDomainCardSelected: () -> Unit,
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
                    DomainCards(onNewDomainCardSelected, onExistingDomainCardSelected)
                    DiscountNotice()
                }
            }
        }
    )
}

@Composable
private fun DomainCards(
    onNewDomainCardSelected: () -> Unit,
    onExistingDomainCardSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(top = 24.dp)) {
        val arrangement = Arrangement.spacedBy(16.dp)
        if (isPortrait) {
            Column(verticalArrangement = arrangement) {
                NewDomainCard(onNewDomainCardSelected)
                ExistingDomainCard(onExistingDomainCardSelected)
            }
        } else {
            Row(horizontalArrangement = arrangement) {
                NewDomainCard(onNewDomainCardSelected, modifier = Modifier.weight(1f))
                ExistingDomainCard(onExistingDomainCardSelected, modifier = Modifier.weight(1f))
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
    onNewDomainCardSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainOptionCard(
        icon = R.drawable.ic_domains_white_24dp,
        title = R.string.purchase_domain_screen_new_domain_card_title,
        description = R.string.purchase_domain_screen_new_domain_card_description,
        button = R.string.purchase_domain_screen_new_domain_card_button,
        onOptionSelected = onNewDomainCardSelected,
        modifier = modifier,
    )
}

@Composable
private fun ExistingDomainCard(
    onExistingDomainCardSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainOptionCard(
        icon = R.drawable.ic_themes_white_24dp,
        title = R.string.purchase_domain_screen_existing_domain_card_title,
        description = R.string.purchase_domain_screen_existing_domain_card_description,
        button = R.string.purchase_domain_screen_existing_domain_card_button,
        onOptionSelected = onExistingDomainCardSelected,
        modifier = modifier,
    )
}

@Composable
private fun DomainOptionCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes button: Int,
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
        Button(
            onClick = onOptionSelected,
            shape = MaterialTheme.shapes.small.copy(CornerSize(36.dp)),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.success
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(button),
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                ),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private val isPortrait: Boolean @Composable get() = LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT

@Preview(name = "Light mode", locale = "en")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small screen", device = Devices.NEXUS_5)
@Preview(name = "Landscape orientation", device = Devices.AUTOMOTIVE_1024p)
@Composable
fun PurchaseDomainScreenPreview() {
    AppTheme {
        PurchaseDomainScreen({}, {}, {})
    }
}

private val decorativeIconContentDescription: String? = null
