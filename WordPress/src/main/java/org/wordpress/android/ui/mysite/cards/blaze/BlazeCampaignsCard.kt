package org.wordpress.android.ui.mysite.cards.blaze

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BlazeCard.BlazeCampaignsCardModel.BlazeCampaignsCardItem.BlazeCampaignStats
import org.wordpress.android.ui.utils.UiString

@Composable
@Suppress("FunctionName")
fun BlazeCampaignsCard(
    blazeCampaignCardModel: BlazeCampaignsCardModel,
    modifier: Modifier = Modifier
) {
    UnelevatedCard(
        modifier = modifier.clickable { blazeCampaignCardModel.onClick.click() },
        content = {
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = uiStringText(uiString = blazeCampaignCardModel.title),
                        style = DashboardCardTypography.smallTitle,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    CardDropDownMenu(moreMenuOptions = blazeCampaignCardModel.moreMenuOptions)
                }
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .clickable {
                            blazeCampaignCardModel.campaign.onClick(blazeCampaignCardModel.campaign.id)
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val status = blazeCampaignCardModel.campaign.status
                    if (status != null) {
                        BlazeStatusLabel(
                            status = status,
                        )
                    }
                    CampaignTitleThumbnail(
                        campaignTitle = blazeCampaignCardModel.campaign.title,
                        featuredImageUrl = blazeCampaignCardModel.campaign.featuredImageUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    if (blazeCampaignCardModel.campaign.stats != null) {
                        CampaignStats(
                            campaignStats = blazeCampaignCardModel.campaign.stats,
                        )
                    } else Spacer(modifier = Modifier.size(8.dp))
                }
                Divider(
                    thickness = 0.5.dp,
                    modifier = Modifier
                        .padding(start = 16.dp)
                )
                Column(modifier = Modifier
                    .clickable { blazeCampaignCardModel.footer.onClick.click() }) {
                    Text(
                        text = uiStringText(uiString = blazeCampaignCardModel.footer.label),
                        style = DashboardCardTypography.footerCTA,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                    )
                }
            }
        },
    )
}

@Composable
fun CampaignTitleThumbnail(campaignTitle: UiString, featuredImageUrl: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = uiStringText(uiString = campaignTitle),
            style = DashboardCardTypography.subTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(
                1f,
                fill = false
            )
        )
        if (featuredImageUrl != null) {
            Spacer(Modifier.width(16.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(featuredImageUrl)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.featured_image_desc),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun CampaignStats(
    campaignStats: BlazeCampaignStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CampaignStat(
            modifier = Modifier
                .weight(1f),
            title = stringResource(id = R.string.impressions_campaign_stats),
            value = campaignStats.impressions
        )
        CampaignStat(
            modifier = Modifier
                .weight(1f),
            title = stringResource(id = R.string.clicks_campaign_stats),
            value = campaignStats.clicks
        )
    }
}

@Composable
private fun CampaignStat(title: String, value: UiString, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = title,
            style = DashboardCardTypography.detailText,
            textAlign = TextAlign.Start
        )
        Text(
            text = uiStringText(uiString = value),
            style = DashboardCardTypography.largeText,
            textAlign = TextAlign.Start
        )
    }
}


@Composable
private fun CardDropDownMenu(moreMenuOptions: BlazeCampaignsCardModel.MoreMenuOptions, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        var isExpanded by remember { mutableStateOf(false) }

        IconButton(onClick = {
            isExpanded = true
            moreMenuOptions.onMoreClick.click()
        }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.more),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier
                .background(MaterialTheme.colors.surface.copy(alpha = ContentAlpha.high))
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.blaze_campaigns_card_more_menu_view_all_campaigns)) },
                onClick = {
                    isExpanded = false
                    moreMenuOptions.viewAllCampaignsItemClick.click()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.blaze_campaigns_card_more_menu_learn_more)) },
                onClick = {
                    isExpanded = false
                    moreMenuOptions.learnMoreClick.click()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.blaze_campaigns_card_more_menu_hide_this)) },
                onClick = {
                    isExpanded = false
                    moreMenuOptions.hideThisMenuItemClick.click()
                }
            )
        }
    }
}
