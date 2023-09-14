package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.cards.blaze.BlazeStatusLabel
import org.wordpress.android.ui.mysite.cards.blaze.CampaignStatus
import org.wordpress.android.ui.utils.UiString

@Composable
fun CampaignListRow(campaignModel: CampaignModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(
                    1f,
                    fill = false
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (campaignModel.status != null) {
                    BlazeStatusLabel(
                        status = campaignModel.status,
                    )
                }
                Text(
                    text = uiStringText(uiString = campaignModel.title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = colors.onSurface.copy(alpha = ContentAlpha.high)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                CampaignStats(
                    budget = campaignModel.budget,
                    impressions = campaignModel.impressions,
                    clicks = campaignModel.clicks
                )
            }
            if (campaignModel.featureImageUrl != null) {
                Spacer(Modifier.width(16.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(campaignModel.featureImageUrl)
                        .crossfade(true)
                        .build(),
                    contentScale = ContentScale.Crop,
                    contentDescription = stringResource(R.string.featured_image_desc),
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .size(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
        Divider(
            thickness = 0.5.dp,
            modifier = Modifier
                .padding()
        )
    }
}

@Composable
private fun CampaignStats(
    budget: UiString,
    impressions: UiString?,
    clicks: UiString?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (impressions != null) {
            CampaignStat(
                modifier = Modifier
                    .weight(1f),
                title = stringResource(id = R.string.impressions_campaign_stats),
                value = impressions
            )
        }
        if (clicks != null) {
            CampaignStat(
                modifier = Modifier
                    .weight(1f),
                title = stringResource(id = R.string.clicks_campaign_stats),
                value = clicks
            )
        }
        CampaignStat(
            modifier = Modifier
                .weight(1f),
            title = stringResource(id = R.string.budget_campaign_stats),
            value = budget
        )
    }
}

@Composable
private fun CampaignStat(title: String, value: UiString, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = uiStringText(uiString = value),
            style = MaterialTheme.typography.headlineSmall.copy(
                color = colors.onSurface.copy(alpha = ContentAlpha.high),
                fontSize = 16.sp,
            ),
            textAlign = TextAlign.Start
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = colors.onSurface.copy(alpha = ContentAlpha.medium)
            ),
            textAlign = TextAlign.Start
        )
    }
}


@Preview
@Composable
fun CampaignListRowPreview() {
    CampaignListRow(
        campaignModel = CampaignModel(
            id = "1",
            title = UiString.UiStringText("This is just a campaign title to see how the UI looks like"),
            status = CampaignStatus.Active,
            featureImageUrl = "https://picsum.photos/200/300",
            impressions = UiString.UiStringText("10,000"),
            clicks = UiString.UiStringText("100"),
            budget = UiString.UiStringText("$100")
        )
    )
}
