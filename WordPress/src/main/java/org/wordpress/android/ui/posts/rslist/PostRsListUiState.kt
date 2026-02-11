package org.wordpress.android.ui.posts.rslist

import org.wordpress.android.ui.posts.rslist.models.PostRsModel
import org.wordpress.android.util.DateTimeUtilsWrapper
import java.text.DateFormat

/**
 * UI state for a single tab in the post list.
 */
data class PostTabUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val posts: List<PostUiModel> = emptyList(),
    val error: String? = null
)

/**
 * Display model for a single post in the list.
 */
data class PostUiModel(
    val remoteId: Long,
    val title: String,
    val excerpt: String,
    val dateFormatted: String,
    val statusLabel: String
)

/**
 * One-time UI events emitted by the ViewModel.
 */
sealed class PostRsListUiEvent {
    data class ShowError(
        val message: String
    ) : PostRsListUiEvent()

    data class OpenPost(
        val remotePostId: Long
    ) : PostRsListUiEvent()
}

// ========== Mapping Functions ==========

fun PostRsModel.toUiModel(
    dateTimeUtilsWrapper: DateTimeUtilsWrapper
): PostUiModel {
    val parsed = dateTimeUtilsWrapper.dateFromIso8601(date)
    val formatted = if (status == "future" && parsed != null) {
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(parsed)
    } else {
        dateTimeUtilsWrapper.javaDateToTimeSpan(parsed)
    }
    return PostUiModel(
        remoteId = remotePostId,
        title = title.ifBlank { "(Untitled)" },
        excerpt = stripHtml(excerpt).take(MAX_EXCERPT_LENGTH),
        dateFormatted = formatted,
        statusLabel = mapStatusLabel(status)
    )
}

private fun mapStatusLabel(status: String): String {
    return when (status) {
        "publish" -> "Published"
        "draft" -> "Draft"
        "pending" -> "Pending Review"
        "private" -> "Private"
        "future" -> "Scheduled"
        "trash" -> "Trashed"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun stripHtml(html: String): String {
    return html.replace(Regex("<[^>]*>"), "").trim()
}

private const val MAX_EXCERPT_LENGTH = 150
