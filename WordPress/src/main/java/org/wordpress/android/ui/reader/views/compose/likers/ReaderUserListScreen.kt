package org.wordpress.android.ui.reader.views.compose.likers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderLiker
import org.wordpress.android.ui.reader.viewmodels.ReaderUserListUiState
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImagePlaceholderManager
import org.wordpress.android.util.image.ImageType

private val avatarSize = 32.dp
private val avatarPlaceholders = ImagePlaceholderManager()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderUserListScreen(
    uiState: ReaderUserListUiState,
    onBack: () -> Unit,
    onLikerClick: (ReaderLiker) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    // empty until the likes have loaded, as before
                    if (uiState.isLoaded) {
                        Text(
                            text = ReaderUtils.getLongLikeLabelText(
                                LocalContext.current,
                                uiState.numLikes,
                                uiState.isLikedByCurrentUser
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        // the Scaffold's content padding already includes the horizontal insets
        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            items(uiState.likers, key = { it.userId }) { liker ->
                LikerRow(liker, onClick = { onLikerClick(liker) })
            }
        }
    }
}

@Composable
private fun LikerRow(liker: ReaderLiker, onClick: () -> Unit) {
    val avatarSizePx = with(LocalDensity.current) { avatarSize.roundToPx() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = liker.blogId != 0L, onClick = onClick)
            .padding(16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(WPAvatarUtils.rewriteAvatarUrl(liker.avatarUrl, avatarSizePx))
                .apply {
                    avatarPlaceholders.getPlaceholderResource(ImageType.AVATAR_WITH_BACKGROUND)?.let { placeholder(it) }
                    avatarPlaceholders.getErrorResource(ImageType.AVATAR_WITH_BACKGROUND)?.let { error(it) }
                }
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = liker.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            liker.urlDomain?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
