package org.wordpress.android.ui.mysite.cards.blaze

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BlazeCard.BlazeCampaignsCard

@Composable
@Suppress("FunctionName")
fun BlazeCampaignsCardView(
    modifier: Modifier = Modifier,
    blazeCampaignCard: BlazeCampaignsCard
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(1.dp),
        border = BorderStroke(
            width = dimensionResource(id = R.dimen.unelevated_card_stroke_width),
            color = colorResource(id = R.color.on_surface_divider)
        ),
        elevation = 0.dp,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Text(
                text = uiStringText(uiString = blazeCampaignCard.title),
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                color = colorResource(R.color.material_on_surface_emphasis_high_type),
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 16.dp, top = 16.dp)
            )
            Text(
                text = uiStringText(uiString = blazeCampaignCard.campaign.status),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(R.color.material_on_surface_emphasis_medium),
                textAlign = TextAlign.Start,
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 16.dp, top = 8.dp)
            )
            Text(
                text = uiStringText(uiString = blazeCampaignCard.campaign.title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start,
                color = colorResource(R.color.material_on_surface_emphasis_medium),
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 16.dp, top = 8.dp)
            )
            OutlinedButton(
                onClick = { blazeCampaignCard.footer.onClick.click() },
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Text(
                    text = uiStringText(uiString = blazeCampaignCard.footer.label),
                    color = colorResource(R.color.material_on_surface_emphasis_medium),
                )
            }
        }
    }
}
