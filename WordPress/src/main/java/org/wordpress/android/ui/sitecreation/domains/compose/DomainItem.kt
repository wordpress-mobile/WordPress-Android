package org.wordpress.android.ui.sitecreation.domains.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.compose.components.SolidCircle
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.asString
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Cost
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variant.BestAlternative
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variant.Recommended
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variant.Sale
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variant.Unavailable

private val SecondaryTextColor @Composable get() = MaterialTheme.colors.onSurface.copy(alpha = 0.46f)
private val SecondaryFontSize = 13.sp
private val PrimaryFontSize = 17.sp

@Composable
fun DomainItem(uiState: DomainUiState) = with(uiState) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onClick.invoke() }
                .padding(vertical = Margin.ExtraLarge.value)
                .padding(end = Margin.ExtraLarge.value)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(Margin.ExtraMediumLarge.value)
            ) {
                variant?.dotColor?.let {
                    SolidCircle(size = 8.dp, colorResource(it))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = domainName,
                    color = MaterialTheme.colors.onSurface.takeIf { variant !is Unavailable } ?: SecondaryTextColor,
                    fontSize = PrimaryFontSize,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                variant?.run {
                    Text(
                        text = subtitle.asString(),
                        color = subtitleColor?.let { colorResource(it) } ?: SecondaryTextColor,
                        fontSize = SecondaryFontSize,
                    )
                }
            }
            if (variant !is Unavailable) {
                if (cost is Cost.OnSale) {
                    SalePrince(
                        cost.title.asString(),
                        cost.subtitle.asString(),
                        modifier = Modifier.padding(start = Margin.ExtraLarge.value)
                    )
                } else {
                    Price(
                        cost.title.asString(),
                        modifier = Modifier.padding(start = Margin.ExtraLarge.value)
                    )
                }
            }
        }
        Divider(thickness = 0.5.dp)
    }
}

@Composable
private fun Price(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = SecondaryTextColor,
        fontSize = PrimaryFontSize,
        modifier = modifier
    )
}

@Composable
private fun SalePrince(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            title,
            color = AppColor.JetpackGreen50,
            fontSize = PrimaryFontSize,
        )
        Text(
            subtitle,
            color = SecondaryTextColor,
            fontSize = SecondaryFontSize,
            textDecoration = TextDecoration.LineThrough,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DomainItemPreview() {
    val uiStates = MutableList(9) {
        DomainUiState(
            domainName = buildString {
                repeat(5) { index -> append('a' + it + index) }
                append(".domain.com")
            },
            cost = when {
                it % 3 == 0 -> Cost.Paid("$${it * 5}")
                it == 5 -> Cost.OnSale("$${it * 2}", "$${it * 3}")
                else -> Cost.Free
            },
            variant = when (it) {
                0 -> Unavailable
                1 -> Recommended
                2 -> BestAlternative
                5 -> Sale
                else -> null
            },
            onClick = {}
        )
    }
    AppTheme {
        Column {
            uiStates.forEach { DomainItem(it) }
        }
    }
}

