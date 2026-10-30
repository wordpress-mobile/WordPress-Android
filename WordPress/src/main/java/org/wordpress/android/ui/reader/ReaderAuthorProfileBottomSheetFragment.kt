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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImagePlaceholderManager
import org.wordpress.android.util.image.ImageType

class ReaderAuthorProfileBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        val authorName = args.getString(ARG_AUTHOR_NAME).orEmpty()
        val authorAvatar = args.getString(ARG_AUTHOR_AVATAR).orEmpty()
        val blogName = args.getString(ARG_BLOG_NAME).orEmpty()
        val blogUrl = args.getString(ARG_BLOG_URL).orEmpty()
        val avatarSizePx = (AVATAR_SIZE.value * resources.displayMetrics.density).toInt()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppThemeM3 {
                    AuthorProfile(
                        authorName = authorName,
                        avatarUrl = WPAvatarUtils.rewriteAvatarUrl(authorAvatar, avatarSizePx),
                        blogName = blogName.ifBlank { UrlUtils.getHost(blogUrl) },
                        onBlogClick = if (blogUrl.isNotBlank()) {
                            {
                                ReaderActivityLauncher.openUrl(context, blogUrl)
                                dismiss()
                            }
                        } else {
                            null
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

private val AVATAR_SIZE = 56.dp
private val avatarPlaceholders = ImagePlaceholderManager()

@Composable
private fun AuthorProfile(authorName: String, avatarUrl: String, blogName: String, onBlogClick: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Margin.ExtraLarge.value, end = Margin.ExtraLarge.value)
            .padding(top = Margin.ExtraMediumLarge.value, bottom = Margin.ExtraLarge.value)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .apply {
                    avatarPlaceholders.getPlaceholderResource(ImageType.AVATAR_WITH_BACKGROUND)
                        ?.let { placeholder(it) }
                    avatarPlaceholders.getErrorResource(ImageType.AVATAR_WITH_BACKGROUND)?.let { error(it) }
                }
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
        )
        Text(
            text = authorName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Margin.Medium.value)
        )
        if (onBlogClick != null) {
            Text(
                text = blogName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = Margin.Small.value)
                    .clickable(onClick = onBlogClick)
                    .padding(Margin.Small.value)
            )
        }
    }
}
