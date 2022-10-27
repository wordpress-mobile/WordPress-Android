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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import org.wordpress.android.R
import org.wordpress.android.ui.compose.modifiers.disableUserScroll
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeViewModel.SiteListItemUiState

@Composable
fun SiteList(
    items: List<SiteListItemUiState>,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                SiteIcon(site.iconUrl)
                Column {
                    SiteName(site.name)
                    SiteAddress(site.url)
                }
            }
            Divider(color = colorResource(R.color.gray_10).copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SiteIcon(
    iconUrl: String
) {
    val painter = rememberImagePainter(iconUrl) {
        placeholder(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp)
        error(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp)
        crossfade(true)
    }
    Image(
            painter = painter,
            contentDescription = stringResource(R.string.blavatar_desc),
            modifier = Modifier
                    .padding(vertical = 15.dp)
                    .padding(end = 20.dp)
                    .size(60.dp)
                    .clip(RoundedCornerShape(3.dp))
    )
}

@Composable
private fun SiteName(name: String) {
    Text(
            text = name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SiteAddress(url: String) {
    Text(
            text = url,
            fontSize = FontSize.Large.value,
            color = colorResource(R.color.gray_40),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
    )
}
