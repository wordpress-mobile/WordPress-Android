package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import coil.compose.AsyncImage
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPAvatarUtils

class ReaderAuthorProfileBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeM3 {
                    val args = requireArguments()
                    ReaderAuthorProfileBottomSheetScreen(
                        authorName = args.getString(ARG_AUTHOR_NAME).orEmpty(),
                        authorAvatar = args.getString(ARG_AUTHOR_AVATAR).orEmpty(),
                        blogName = args.getString(ARG_BLOG_NAME).orEmpty(),
                        blogUrl = args.getString(ARG_BLOG_URL).orEmpty(),
                        onBlogClick = { url ->
                            ReaderActivityLauncher.openUrl(
                                requireContext(), url
                            )
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "ReaderAuthorProfileBottomSheetFragment"
        private const val ARG_AUTHOR_NAME = "author_name"
        private const val ARG_AUTHOR_AVATAR = "author_avatar"
        private const val ARG_BLOG_NAME = "blog_name"
        private const val ARG_BLOG_URL = "blog_url"

        fun newInstance(
            authorName: String,
            authorAvatar: String,
            blogName: String,
            blogUrl: String,
        ) = ReaderAuthorProfileBottomSheetFragment().apply {
            arguments = bundleOf(
                ARG_AUTHOR_NAME to authorName,
                ARG_AUTHOR_AVATAR to authorAvatar,
                ARG_BLOG_NAME to blogName,
                ARG_BLOG_URL to blogUrl,
            )
        }
    }
}

@Composable
private fun ReaderAuthorProfileBottomSheetScreen(
    authorName: String,
    authorAvatar: String,
    blogName: String,
    blogUrl: String,
    onBlogClick: (String) -> Unit = {},
) {
    val avatarSizeDp = 56.dp
    val avatarSizePx = with(LocalDensity.current) {
        avatarSizeDp.roundToPx()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        AsyncImage(
            model = WPAvatarUtils.rewriteAvatarUrl(
                authorAvatar, avatarSizePx
            ),
            contentDescription = null,
            modifier = Modifier
                .size(avatarSizeDp)
                .clip(CircleShape)
        )
        Text(
            text = authorName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        if (blogUrl.isNotBlank()) {
            Text(
                text = blogName.ifBlank {
                    UrlUtils.getHost(blogUrl)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBlogClick(blogUrl) }
                    .padding(top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewAuthorProfileBottomSheet() {
    AppThemeM3 {
        ReaderAuthorProfileBottomSheetScreen(
            authorName = "Jane Doe",
            authorAvatar = "",
            blogName = "My Awesome Blog",
            blogUrl = "https://example.com",
        )
    }
}
