package org.wordpress.android.ui.sitecreation.domains.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.asString
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Cost
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variation.BestAlternative
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variation.Recommended
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variation.Sale
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Variation.Unavailable

private val SecondaryTextColor @Composable get() = MaterialTheme.colors.onSurface.copy(alpha = 0.46f)

@Composable
fun DomainItem(uiState: DomainUiState) = with(uiState) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onClick.invoke() }
                .padding(vertical = 16.dp)
                .padding(end = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(24.dp)
            ) {
                variant?.dotColor?.let {
                    DotIndicator(colorResource(it))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = domainName,
                    color = MaterialTheme.colors.onSurface.takeIf { variant !is Unavailable } ?: SecondaryTextColor,
                    fontSize = 17.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                variant?.run {
                    Text(
                        text = subtitle.asString(),
                        color = subtitleColor?.let { colorResource(it) } ?: SecondaryTextColor,
                        fontSize = 13.sp,
                    )
                }
            }
            if (variant !is Unavailable) {
                Price(cost.uiString.asString(), modifier = Modifier.padding(start = 16.dp))
            }
        }
        Divider(thickness = 0.5.dp)
    }
}

@Composable
private fun DotIndicator(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Suppress("ForbiddenComment")
@Composable
private fun Price(text: String, modifier: Modifier = Modifier) {
    // TODO: Style price:
    //  - Free: "Free"
    //  - Paid: "$cost/yr"
    //  - Sale: "$costDiscounted/yr" [JP Green 50, 17 regular] + \n + "$cost/yr" [strikethrough, 13 regular]
    Text(
        text = text,
        color = SecondaryTextColor,
        fontSize = 17.sp,
        modifier = modifier
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DomainItemPreview() {
    val uiStates = MutableList(9) {
        DomainUiState(
            domainName = buildString {
                repeat(25) { index -> append('a' + it + index) }
                append(".domain.com")
            },
            cost = if (it % 3 == 0) Cost.Paid("$${it * 5}") else Cost.Free,
            variant = when (it) {
                0 -> Unavailable
                1 -> Recommended
                2 -> BestAlternative
                4 -> Sale
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

