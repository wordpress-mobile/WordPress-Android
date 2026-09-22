package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * "In reply to {author}: {snippet}" for a comment that answers another one, matching iOS's
 * `CommentParentStrip`.
 *
 * Unlike iOS this does not navigate to the parent - the Android detail is a pager over the list's
 * own comments, so pushing an arbitrary parent onto it has nowhere to come back to. It is context
 * only, which is what the strip is mostly read as anyway.
 */
@Composable
fun CommentParentStrip(
    parentAuthorName: String,
    parentSnippet: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ICON_SIZE)
        )
        Text(
            text = buildAnnotatedString {
                val prefix = stringResource(R.string.comment_detail_in_reply_to, parentAuthorName)
                append(prefix)
                addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), 0, prefix.length)
                if (parentSnippet.isNotBlank()) {
                    append(" ")
                    append(parentSnippet)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = ICON_GAP)
        )
    }
}

private val ICON_SIZE = 16.dp
private val ICON_GAP = 8.dp

@Preview(showBackground = true)
@Composable
private fun CommentParentStripPreview() {
    AppThemeM3 {
        CommentParentStrip(
            parentAuthorName = "Jane Doe",
            parentSnippet = "Thanks for writing this up, it cleared a lot up for me."
        )
    }
}
