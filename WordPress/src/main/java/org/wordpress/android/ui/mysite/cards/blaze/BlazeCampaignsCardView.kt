package org.wordpress.android.ui.mysite.cards.blaze

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.DashboardCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard.BlazeCampaignsCardItem.BlazeCampaignStats
import org.wordpress.android.ui.utils.UiString

@Composable
@Suppress("FunctionName")
fun BlazeCampaignsCardView(
    modifier: Modifier = Modifier,
    blazeCampaignCard: BlazeCampaignsCard,
    isInDarkMode: Boolean = isSystemInDarkTheme()
) {
    DashboardCard(modifier = modifier, content = {
        Text(
            text = uiStringText(uiString = blazeCampaignCard.title),
            style = DashboardCardTypography.smallTitle,
            textAlign = TextAlign.Start,
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 16.dp, top = 16.dp)
        )
        val status = blazeCampaignCard.campaign.status
        if (status != null) {
            BlazeStatusLabel(
                status = status,
                isInDarkMode = isInDarkMode
            )
        }
        CampaignTitleThumbnail(
            campaignTitle = blazeCampaignCard.campaign.title,
            featuredImageUrl = blazeCampaignCard.campaign.featuredImageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
        if (blazeCampaignCard.campaign.stats != null) {
            CampaignStats(
                campaignStats = blazeCampaignCard.campaign.stats,
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .background(Color.LightGray)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
        Text(
            text = uiStringText(uiString = blazeCampaignCard.footer.label),
            style = DashboardCardTypography.footerCTA,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
        )
    })
}

@Composable
fun CampaignTitleThumbnail(campaignTitle: UiString, featuredImageUrl: String?, modifier: Modifier = Modifier) {
    ConstraintLayout(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val (container, featuredImage, title) = createRefs()
        Box(modifier = Modifier
            .constrainAs(container) {
                top.linkTo(parent.top, 0.dp)
                bottom.linkTo(parent.bottom, 0.dp)
                start.linkTo(parent.start, 0.dp)
                end.linkTo(parent.end, 0.dp)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
        )
        FeaturedImage(url = featuredImageUrl, modifier = Modifier
            .constrainAs(featuredImage) {
                top.linkTo(container.top)
                bottom.linkTo(container.bottom)
                end.linkTo(container.end)
            })
        CampaignTitle(
            title = uiStringText(uiString = campaignTitle), modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(container.top)
                    start.linkTo(container.start)
                    featuredImageUrl?.run {
                        end.linkTo(featuredImage.start, margin = 16.dp)
                    } ?: run {
                        end.linkTo(container.end, margin = 16.dp)
                    }
                    width = Dimension.fillToConstraints
                }
                .wrapContentHeight()
        )
    }
}

@Composable
private fun FeaturedImage(url: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentScale = ContentScale.Crop,
        contentDescription = stringResource(R.string.featured_image_desc),
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

@Composable
private fun CampaignTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = DashboardCardTypography.subTitle,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.wrapContentHeight()
    )
}

@Composable
private fun CampaignStats(
    campaignStats: BlazeCampaignStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        CampaignStat(
            modifier = modifier
                .wrapContentHeight()
                .wrapContentWidth(),
            title = "Impressions",
            value = campaignStats.impressions
        )
        CampaignStat(
            modifier = modifier
                .wrapContentHeight()
                .wrapContentWidth()
                .padding(end = 8.dp),
            title = "Clicks",
            value = campaignStats.clicks
        )
    }
}

@Composable
private fun CampaignStat(title: String, value: UiString, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentWidth()
    ) {
        Text(
            text = title,
            style = DashboardCardTypography.detailText,
            textAlign = TextAlign.Start,
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
        )
        Text(
            text = uiStringText(uiString = value),
            style = DashboardCardTypography.largeText,
            textAlign = TextAlign.Start,
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
        )
    }
}
