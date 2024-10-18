package org.wordpress.android.ui.main.jetpack.migration.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.text.MessageM3
import org.wordpress.android.ui.compose.components.text.SubtitleM3
import org.wordpress.android.ui.compose.components.text.TitleM3
import org.wordpress.android.ui.compose.modifiers.conditionalThen
import org.wordpress.android.ui.compose.modifiers.disableUserScroll
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.SiteListItemUiState
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState
import org.wordpress.android.ui.main.jetpack.migration.compose.dimmed

@Composable
fun SiteList(
    uiState: UiState.Content.Welcome,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    blurModifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    bottomPaddingPx: Int = 0,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .conditionalThen(!userScrollEnabled, Modifier.disableUserScroll())
            .background(MaterialTheme.colorScheme.background)
            .fillMaxHeight()
            .then(blurModifier),
    ) {
        item {
            SiteListHeader(uiState)
        }
        items(
            items = uiState.sites,
            key = { it.id },
        ) { site ->
            SiteListItem(
                uiState = site,
                isDimmed = uiState.isProcessing,
            )
            HorizontalDivider(
                color = colorResource(R.color.gray_10),
                thickness = 0.5.dp,
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.jp_migration_padding_horizontal))
                    .dimmed(uiState.isProcessing),
            )
        }
        item {
            val bottomPadding = LocalDensity.current.run { bottomPaddingPx.toDp() + 30.dp }
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun SiteListItem(uiState: SiteListItemUiState, isDimmed: Boolean): Unit = with(uiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = dimensionResource(R.dimen.jp_migration_padding_horizontal))
            .dimmed(isDimmed),
    ) {
        SiteIcon(iconUrl)
        Column {
            SiteName(name)
            SiteAddress(url)
        }
    }
}

@Composable
private fun SiteListHeader(uiState: UiState.Content.Welcome): Unit = with(uiState) {
    Column(
        modifier = Modifier
            .dimmed(uiState.isProcessing)
    ) {
        ScreenIcon(iconRes = screenIconRes)
        TitleM3(text = uiStringText(title))
        SubtitleM3(text = uiStringText(subtitle))
        MessageM3(text = uiStringText(message))
    }
}

@Composable
private fun SiteIcon(iconUrl: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(iconUrl)
            .error(R.drawable.ic_site_icon_placeholder_primary_24)
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.blavatar_desc),
        modifier = Modifier
            .padding(vertical = 15.dp)
            .padding(end = 20.dp)
            .size(dimensionResource(R.dimen.jp_migration_site_icon_size))
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
