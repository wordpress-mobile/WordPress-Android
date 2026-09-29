package org.wordpress.android.ui.commentsrs.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.commentsrs.CommentRsUiModel

private val AVATAR_SIZE = 40.dp

@Composable
internal fun CommentAvatar(comment: CommentRsUiModel, isSelected: Boolean, onClick: () -> Unit) {
    Crossfade(targetState = isSelected, label = "avatar") { selected ->
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.comment_checkmark_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
            )
        } else {
            val fallback = rememberVectorPainter(Icons.Filled.Person)
            AsyncImage(
                model = comment.avatarUrl.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                fallback = fallback,
                error = fallback,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
            )
        }
    }
}

/**
 * The row title: "{author} on {post title}" via the shared [R.string.comment_title] template with
 * both parts bold (like legacy `CommentListUiUtils.formatCommentTitle`), or just the bold author
 * name while the post title is unresolved.
 */
@Composable
internal fun commentTitle(comment: CommentRsUiModel): AnnotatedString {
    val postTitle = comment.postTitle?.trim().orEmpty()
    val formatted = if (postTitle.isEmpty()) {
        comment.authorName
    } else {
        stringResource(R.string.comment_title, comment.authorName, postTitle)
    }
    return buildAnnotatedString {
        append(formatted)
        boldRange(formatted, comment.authorName)
        if (postTitle.isNotEmpty()) boldRange(formatted, postTitle)
    }
}

private fun AnnotatedString.Builder.boldRange(formatted: String, part: String) {
    val start = formatted.indexOf(part)
    if (start >= 0) {
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + part.length)
    }
}
