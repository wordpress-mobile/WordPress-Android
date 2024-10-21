package org.wordpress.android.ui.sitecreation.domains.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.compose.components.SolidCircle
import org.wordpress.android.ui.compose.theme.AppThemeM2WithoutBackground
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.asString
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Cost
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Tag.BestAlternative
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Tag.Recommended
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Tag.Sale
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Tag.Unavailable
import androidx.compose.ui.R as ComposeR

private val HighlightBgColor @Composable get() = colors.primary.copy(0.1f)
private val SecondaryTextColor @Composable get() = colors.onSurface.copy(0.46f)
private val SecondaryFontSize = 13.sp
private val PrimaryFontSize = 17.sp
private val StartPadding = 40.dp


@Suppress("CyclomaticComplexMethod")
@Composable
fun DomainItem(uiState: DomainUiState): Unit = with(uiState) {
    Column(Modifier.background(if (isSelected) HighlightBgColor else Unspecified)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = remember(::MutableInteractionSource),
                    indication = rememberRipple(color = HighlightBgColor),
                    onClick = onClick::invoke,
                )
                .then(if (cost is Cost.Paid) {
                    Modifier.padding(top = Margin.ExtraLarge.value)
                } else {
                    Modifier.padding(vertical = Margin.ExtraLarge.value)
                })
                .padding(end = Margin.ExtraLarge.value)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(StartPadding)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(ComposeR.string.selected),
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    tags.firstOrNull()?.dotColor?.let {
                        SolidCircle(size = 8.dp, colorResource(it))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = domainName,
                    color = colors.onSurface.takeIf { tags.none { it is Unavailable } } ?: SecondaryTextColor,
                    fontSize = PrimaryFontSize,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                tags.forEach { tag ->
                    tag.run {
                        Text(
                            text = subtitle.asString(),
                            color = subtitleColor?.let { colorResource(it) } ?: SecondaryTextColor,
                            fontSize = SecondaryFontSize,
                        )
                    }
                }
            }
            if (tags.none { it is Unavailable }) {
                if (cost is Cost.OnSale) {
                    SalePrice(
                        cost.strikeoutTitle.asString() to cost.title.asString(),
                        cost.subtitle.asString(),
                        modifier = Modifier.padding(start = Margin.ExtraLarge.value)
                    )
                }
                else if (cost is Cost.Paid) {
                    Plan(
                        cost.strikeoutTitle.asString() to cost.title.asString(),
                        modifier = Modifier.padding(start = Margin.ExtraLarge.value)
                    )
                }
                else {
                    Price(
                        cost.title.asString(),
                        modifier = Modifier.padding(start = Margin.ExtraLarge.value)
                    )
                }
            }
        }
        if (cost is Cost.Paid) {
            Row(modifier = Modifier.padding(bottom = Margin.ExtraLarge.value, start = StartPadding)) {
                Text(
                    text = cost.subtitle.asString(),
                    color = colors.primary,
                    fontSize = SecondaryFontSize,
                )
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
private fun SalePrice(title: Pair<String, String>, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.End,
    ) {
        title.let { (strikethroughText, normalText) ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    strikethroughText,
                    color = SecondaryTextColor,
                    fontSize = SecondaryFontSize,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    normalText,
                    color = colors.primary,
                    fontSize = PrimaryFontSize,
                )
            }
        }
        Text(
            subtitle,
            color = colors.primary,
            fontSize = SecondaryFontSize,
        )
    }
}

@Composable
private fun Plan(title: Pair<String, String>, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.End,
    ) {
        title.let { (strikethroughText) ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    strikethroughText,
                    color = SecondaryTextColor,
                    fontSize = SecondaryFontSize,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
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
                it in 1..2 -> Cost.OnSale("$${it * 2}", "$${it * 3}")
                else -> Cost.Free
            },
            tags = listOfNotNull(
                when (it) {
                    0 -> Unavailable
                    1 -> Recommended
                    2 -> BestAlternative
                    else -> null
                },
                if (it in 1..2) Sale else null,
            ),
            isSelected = it == 5,
            onClick = {}
        )
    }
    AppThemeM2WithoutBackground {
        Column {
            uiStates.forEach { DomainItem(it) }
        }
    }
}

