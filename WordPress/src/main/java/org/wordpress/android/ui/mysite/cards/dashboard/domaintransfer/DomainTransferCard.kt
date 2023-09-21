package org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainTransferCardModel
import org.wordpress.android.ui.utils.ListItemInteraction

@Composable
fun DomainTransferCard(
    domainTransferCardModel: DomainTransferCardModel,
    modifier: Modifier = Modifier
) {
    UnelevatedCard(
        modifier = modifier.clickable { domainTransferCardModel.onClick.click() },
        content = {
            Column {
                Row(
                    Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = domainTransferCardModel.title),
                        style = DashboardCardTypography.smallTitle,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box() {
                        var isExpanded by remember { mutableStateOf(false) }

                        IconButton(onClick = {
                            isExpanded = true
                            domainTransferCardModel.onMoreMenuClick.click()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(id = R.string.more),
                                tint = MaterialTheme.colors.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.domain_transfer_card_more_menu_hide_this)) },
                                onClick = { domainTransferCardModel.onHideMenuItemClick.click() }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_domain_transfer_54x32),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        alignment = Alignment.Center
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 28.dp),
                        text = stringResource(id = domainTransferCardModel.subtitle),
                        style = DashboardCardTypography.subTitle
                    )
                }

                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = stringResource(id = domainTransferCardModel.caption),
                    textAlign = TextAlign.Start,
                    style = TextStyle(color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium))
                )
                TextButton(
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    onClick = { domainTransferCardModel.onClick.click() }
                ) {
                    Text(
                        text = stringResource(id = domainTransferCardModel.cta),
                        style = DashboardCardTypography.footerCTA
                    )
                }
            }
        })
}

@Preview(
    name = "Light Mode"
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
internal fun DomainTransferCardPreview() {
    AppTheme {
        Surface {
            DomainTransferCard(
                domainTransferCardModel = DomainTransferCardModel(
                    title = R.string.domain_transfer_card_title,
                    subtitle = R.string.domain_transfer_card_sub_title,
                    caption = R.string.domain_transfer_card_caption,
                    cta = R.string.domain_transfer_card_cta,
                    onClick = ListItemInteraction.create { },
                    onHideMenuItemClick = ListItemInteraction.create { },
                    onMoreMenuClick = ListItemInteraction.create { },
                )
            )
        }
    }
}
