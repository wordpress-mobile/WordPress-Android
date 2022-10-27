package org.wordpress.android.ui.main.jetpack.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import org.wordpress.android.R
import org.wordpress.android.ui.compose.modifiers.disableUserScroll
import org.wordpress.android.ui.compose.unit.FontSize.Large
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.SiteListItem

@Composable
fun SiteList(
    items: List<SiteListItem>,
    listState: LazyListState,
    userScrollEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    blurModifier: Modifier = Modifier,
) {
    LazyColumn(
            state = listState,
            modifier = modifier
                    .composed { if (userScrollEnabled) this else disableUserScroll() }
                    .background(colorResource(R.color.white))
                    .padding(horizontal = 30.dp)
                    .then(blurModifier),
    ) {
        items(
                items = items,
                key = { it.id },
        ) { site ->
            Row(
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                val painter = rememberImagePainter(site.iconUrl) {
                    placeholder(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp)
                    crossfade(true)
                }
                Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                                .padding(vertical = 15.dp)
                                .padding(end = 20.dp)
                                .size(60.dp)
                                .clip(RoundedCornerShape(3.dp))
                )
                Column {
                    Text(
                            text = site.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                    )
                    Text(
                            text = site.url,
                            fontSize = Large.value,
                            color = colorResource(R.color.gray_40)
                    )
                }
            }
            Divider(color = colorResource(R.color.gray_10).copy(alpha = 0.5f))
        }
    }
}
