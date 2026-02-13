package org.wordpress.android.ui.postsrs

import android.text.format.DateUtils
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState
import java.text.SimpleDateFormat
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
        date = post.date.toRelativeDate()
    )
}

private val postDateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
}

@Suppress("TooGenericExceptionCaught")
private fun String.toRelativeDate(): String {
    val millis = if (isNotBlank()) {
        // post.date is in site-local time, not UTC
        try {
            postDateFormat.get()?.parse(this)?.time
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }
    return if (millis != null) {
        DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE or DateUtils.FORMAT_ABBREV_MONTH
        ).toString()
    } else {
        this
    }
}
