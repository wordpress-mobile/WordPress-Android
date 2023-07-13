package org.wordpress.android.ui.mysite.cards.blaze

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BlazeStatusLabel(
    modifier: Modifier = Modifier,
    status: CampaignStatus,
    isInDarkMode: Boolean = false
) {
    Box(
        modifier = modifier
            .padding(start = 16.dp, top = 16.dp)
            .background(
                color = status.textViewBackgroundColor(isInDarkMode),
                shape = RoundedCornerShape(size = 2.dp)
            )
            .wrapContentSize(Alignment.CenterStart)
    ) {
        Text(
            text = stringResource(id = status.stringResource),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = status.textColor(isInDarkMode),
            textAlign = TextAlign.Start,
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(
                    start = 4.dp,
                    end = 4.dp,
                    top = 2.dp,
                    bottom = 2.dp
                )
        )
    }
}