package org.wordpress.android.ui.mysite.cards.personalize

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.mysite.MySiteCardAndItem

@Composable
@Suppress("FunctionName")
fun PersonalizeCard(
    model: MySiteCardAndItem.Card.PersonalizeCardModel,
    modifier: Modifier = Modifier
) {
    UnelevatedCard(
        modifier = modifier.clickable { model.onClick.invoke() },
        content = {
            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            )
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = stringResource(id = R.string.personalize_card_text),
                        style = DashboardCardTypography.standaloneText,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        painter = painterResource(id = R.drawable.ic_settings_white_24dp),
                        contentDescription = stringResource(id = R.string.personalize_card_content_description),
                        contentScale = ContentScale.None,
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface)
                    )
                }
            }
        })
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PersonalizeCardPreview() {
    PersonalizeCard(
        model = MySiteCardAndItem.Card.PersonalizeCardModel(
            onClick = { })
    )
}
