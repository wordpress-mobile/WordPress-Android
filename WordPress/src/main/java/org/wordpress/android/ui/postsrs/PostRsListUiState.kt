package org.wordpress.android.ui.postsrs

import org.wordpress.android.R
import org.wordpress.android.ui.postsrs.models.PostRsModel
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.viewmodel.ResourceProvider
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
    dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    resourceProvider: ResourceProvider
): PostUiModel {
    val parsed = DateTimeUtils.dateUTCFromIso8601(
        normalizeIso8601(date)
    )
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
        title = title.ifBlank {
            resourceProvider.getString(
                R.string.untitled_in_parentheses
            )
        },
        excerpt = HtmlUtils.fastStripHtml(excerpt)
            .take(MAX_EXCERPT_LENGTH),
        dateFormatted = formatted,
        statusLabel = mapStatusLabel(status, resourceProvider)
    )
}

private fun mapStatusLabel(
    status: String,
    resourceProvider: ResourceProvider
): String {
    return when (status) {
        "publish" -> resourceProvider.getString(
            R.string.post_status_post_published
        )
        "draft" -> resourceProvider.getString(
            R.string.post_status_draft
        )
        "pending" -> resourceProvider.getString(
            R.string.post_status_pending_review
        )
        "private" -> resourceProvider.getString(
            R.string.post_status_post_private
        )
        "future" -> resourceProvider.getString(
            R.string.post_status_post_scheduled
        )
        "trash" -> resourceProvider.getString(
            R.string.post_status_post_trashed
        )
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun normalizeIso8601(date: String): String {
    return if (date.contains("+") || date.endsWith("Z")) {
        date
    } else {
        "${date}+0000"
    }
}

private const val MAX_EXCERPT_LENGTH = 150
