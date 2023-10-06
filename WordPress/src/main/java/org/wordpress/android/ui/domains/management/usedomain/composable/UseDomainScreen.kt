package org.wordpress.android.ui.domains.management.usedomain.composable

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Icon
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

@Composable
fun UseDomainScreen(
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
                title = stringResource(id = R.string.use_domain_screen_title),
                navigationIcon = NavigationIcons.BackIcon,
                elevation = elevation.value
            )
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
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
                    DomainCards()
                    Spacer(modifier = Modifier.weight(1f))
                    DiscountNotice()
                }
            }
        }
    )
}

@Composable
private fun DomainCards(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(top = 24.dp)) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column {
                NewDomainCard()
                ExistingDomainCard(modifier = Modifier.padding(top = 16.dp))
            }
        } else {
            Row {
                NewDomainCard(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                ExistingDomainCard(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScreenHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.use_domain_screen_header),
        style = MaterialTheme.typography.h4.copy(
            color = MaterialTheme.colors.onSurface
        ),
        modifier = modifier
    )
}

@Composable
private fun ScreenDescription(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.use_domain_screen_description),
        style = MaterialTheme.typography.body1.copy(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        ),
        modifier = modifier.padding(top = 8.dp)
    )
}

@Composable
private fun DiscountNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.use_domain_screen_discount_notice),
        style = MaterialTheme.typography.body2.copy(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        ),
        modifier = modifier.padding(top = 16.dp),
    )
}

@Composable
private fun NewDomainCard(modifier: Modifier = Modifier) {
    DomainOptionCard(
        icon = R.drawable.ic_domains_white_24dp,
        title = R.string.use_domain_screen_new_domain_card_title,
        description = R.string.use_domain_screen_new_domain_card_description,
        button = R.string.use_domain_screen_new_domain_card_button,
        modifier = modifier,
    )
}

@Composable
private fun ExistingDomainCard(modifier: Modifier = Modifier) {
    DomainOptionCard(
        icon = R.drawable.ic_themes_white_24dp,
        title = R.string.use_domain_screen_existing_domain_card_title,
        description = R.string.use_domain_screen_existing_domain_card_description,
        button = R.string.use_domain_screen_existing_domain_card_button,
        modifier = modifier,
    )
}

@Composable
private fun DomainOptionCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes button: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                width = 0.5.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = decorativeIconContentDescription,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(36.dp)
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(id = title),
            style = MaterialTheme.typography.h5.copy(
                color = MaterialTheme.colors.onSurface
            )
        )
        Text(
            text = stringResource(id = description),
            style = MaterialTheme.typography.body1.copy(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { },
            shape = MaterialTheme.shapes.small.copy(CornerSize(36.dp)),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                text = stringResource(button),
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.body1.copy(
                    color = Color.White,
                ),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(name = "Light mode", locale = "en")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Landscape orientation", device = Devices.AUTOMOTIVE_1024p)
@Composable
fun UseDomainScreenPreview() {
    AppTheme {
        UseDomainScreen()
    }
}

private val decorativeIconContentDescription: String? = null
