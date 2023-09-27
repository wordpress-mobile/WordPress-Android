package org.wordpress.android.ui.mysite.cards.nocards

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.utils.UiString

@Composable
@Suppress("FunctionName")
fun NoCardsMessage(
    model: MySiteCardAndItem.Card.NoCardsMessage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = uiStringText(uiString = model.title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.high
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = uiStringText(uiString = model.message),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NoCardsMessagePreview() {
    NoCardsMessage(
        model = MySiteCardAndItem.Card.NoCardsMessage(
            title = UiString.UiStringRes(R.string.my_site_dashboard_no_cards_message_title),
            message = UiString.UiStringRes(R.string.my_site_dashboard_no_cards_message_description)
        )
    )
}
