package org.wordpress.android.viewmodel.posts

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.PERMISSION_ERROR
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class PostListEmptyUiState(
    val title: UiString? = null,
    @DrawableRes val imgResId: Int? = null,
    val buttonText: UiString? = null,
    val onButtonClick: (() -> Unit)? = null,
    val emptyViewVisible: Boolean = true
) {
    class EmptyList(
        title: UiString,
        buttonText: UiString? = null,
        onButtonClick: (() -> Unit)? = null,
        @DrawableRes imageResId: Int = R.drawable.img_illustration_posts_75dp
    ) : PostListEmptyUiState(
            title = title,
            imgResId = imageResId,
            buttonText = buttonText,
            onButtonClick = onButtonClick
    )

    object DataShown : PostListEmptyUiState(emptyViewVisible = false)

    object Loading : PostListEmptyUiState(
            title = UiStringRes(R.string.posts_fetching),
            imgResId = R.drawable.img_illustration_posts_75dp
    )

    class RefreshError(
        title: UiString,
        buttonText: UiString? = null,
        onButtonClick: (() -> Unit)? = null
    ) : PostListEmptyUiState(
            title = title,
            imgResId = R.drawable.img_illustration_empty_results_216dp,
            buttonText = buttonText,
            onButtonClick = onButtonClick
    )

    object PermissionsError : PostListEmptyUiState(
            title = UiStringRes(R.string.error_refresh_unauthorized_posts),
            imgResId = R.drawable.img_illustration_posts_75dp
    )
}

@Suppress("LongParameterList")
fun createEmptyUiState(
    postListType: PostListType,
    isNetworkAvailable: Boolean,
    isLoadingData: Boolean,
    isListEmpty: Boolean,
    isSearchPromptRequired: Boolean,
    error: ListError?,
    fetchFirstPage: () -> Unit,
    newPost: () -> Unit
): PostListEmptyUiState {
    return if (isListEmpty) {
        when {
            error != null -> createErrorListUiState(
                    isNetworkAvailable = isNetworkAvailable,
                    error = error,
                    fetchFirstPage = fetchFirstPage
            )
            isLoadingData -> {
                // don't show intermediate screen when loading search results
                if (postListType == SEARCH) {
                    PostListEmptyUiState.DataShown
                } else {
                    PostListEmptyUiState.Loading
                }
            }
            else -> createEmptyListUiState(
                    postListType = postListType,
                    newPost = newPost,
                    isSearchPromptRequired = isSearchPromptRequired
            )
        }
    } else {
        PostListEmptyUiState.DataShown
    }
}

private fun createErrorListUiState(
    isNetworkAvailable: Boolean,
    error: ListError,
    fetchFirstPage: () -> Unit
): PostListEmptyUiState {
    return if (error.type == PERMISSION_ERROR) {
        PostListEmptyUiState.PermissionsError
    } else {
        val errorText = if (isNetworkAvailable) {
            UiStringRes(R.string.error_refresh_posts)
        } else {
            UiStringRes(R.string.no_network_message)
        }
        PostListEmptyUiState.RefreshError(
                errorText,
                UiStringRes(R.string.retry),
                fetchFirstPage
        )
    }
}

private fun createEmptyListUiState(
    postListType: PostListType,
    newPost: () -> Unit,
    isSearchPromptRequired: Boolean
): PostListEmptyUiState.EmptyList {
    return when (postListType) {
        PUBLISHED -> PostListEmptyUiState.EmptyList(
                UiStringRes(R.string.posts_published_empty),
                UiStringRes(R.string.posts_empty_list_button),
                newPost
        )
        DRAFTS -> PostListEmptyUiState.EmptyList(
                UiStringRes(R.string.posts_draft_empty),
                UiStringRes(R.string.posts_empty_list_button),
                newPost
        )
        SCHEDULED -> PostListEmptyUiState.EmptyList(
                UiStringRes(R.string.posts_scheduled_empty),
                UiStringRes(R.string.posts_empty_list_button),
                newPost
        )
        SEARCH -> {
            val messageResId = if (isSearchPromptRequired) {
                R.string.post_list_search_prompt
            } else {
                R.string.post_list_search_nothing_found
            }

            PostListEmptyUiState.EmptyList(title = UiStringRes(messageResId), imageResId = 0)
        }
        TRASHED -> PostListEmptyUiState.EmptyList(UiStringRes(R.string.posts_trashed_empty))
    }
}
