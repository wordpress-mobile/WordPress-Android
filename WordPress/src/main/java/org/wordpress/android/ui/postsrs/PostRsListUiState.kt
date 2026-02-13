package org.wordpress.android.ui.postsrs

import android.text.format.DateUtils
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostStatus
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PostTabUiState(
    val posts: List<PostRsUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null
)

data class PostRsUiModel(
    val remotePostId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    val isPlaceholder: Boolean = false,
    val isError: Boolean = false
)

fun PostItemState.toUiModel(postId: Long): PostRsUiModel {
    return when (this) {
        is PostItemState.Fresh -> data.toUiModel()
        is PostItemState.Stale -> data.toUiModel()
        is PostItemState.FetchingWithData -> data.toUiModel()
        is PostItemState.FailedWithData -> data.toUiModel()
        is PostItemState.Missing,
        is PostItemState.Fetching -> PostRsUiModel(
            remotePostId = postId,
            title = "",
            excerpt = "",
            date = "",
            isPlaceholder = true
        )
        is PostItemState.Failed -> PostRsUiModel(
            remotePostId = postId,
            title = "",
            excerpt = "",
            date = "",
            isError = true
        )
    }
}

private fun FullEntityAnyPostWithEditContext.toUiModel():
        PostRsUiModel {
    val post: AnyPostWithEditContext = data
    return PostRsUiModel(
        remotePostId = post.id,
        title = post.title?.raw?.takeIf { it.isNotBlank() }
            ?: post.title?.rendered
            ?: "",
        excerpt = (
            post.excerpt?.raw?.takeIf { it.isNotBlank() }
                ?: post.excerpt?.rendered
                ?: ""
            ).let { HtmlUtils.fastStripHtml(it).trim() },
        date = post.date.formatPostDate(post.status)
    )
}

private val postDateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
}

private const val SECONDS_PER_DAY = 60L * 60 * 24
private const val DAYS_PER_YEAR = 365L

/**
 * Formats a post date string to match the old post list behavior:
 * - Scheduled posts always show an abbreviated absolute date
 * - Other posts < 1 year ago show relative time ("2 hr. ago")
 * - Older posts show an abbreviated absolute date ("Jan 2, 2023")
 */
@Suppress("TooGenericExceptionCaught")
private fun String.formatPostDate(status: PostStatus?): String {
    if (isBlank()) return this
    val millis = try {
        postDateFormat.get()?.parse(this)?.time
    } catch (_: Exception) {
        null
    } ?: return this

    if (status is PostStatus.Future) {
        return formatAbbrDate(millis)
    }

    val now = System.currentTimeMillis()
    val secondsSince = (now - millis) / 1000
    if (secondsSince < SECONDS_PER_DAY * DAYS_PER_YEAR) {
        return DateUtils.getRelativeTimeSpanString(
            millis,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        ).toString()
    }
    return formatAbbrDate(millis)
}

private fun formatAbbrDate(millis: Long): String {
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    return dateFormat.format(Date(millis))
}
