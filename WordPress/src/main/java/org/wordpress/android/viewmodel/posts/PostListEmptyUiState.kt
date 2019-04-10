package org.wordpress.android.viewmodel.posts

import android.support.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.R.string
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
        onButtonClick: (() -> Unit)? = null
    ) : PostListEmptyUiState(
            title = title,
            imgResId = R.drawable.img_illustration_posts_75dp,
            buttonText = buttonText,
            onButtonClick = onButtonClick
    )

    object DataShown : PostListEmptyUiState(emptyViewVisible = false)

    object Loading : PostListEmptyUiState(
            title = UiStringRes(string.posts_fetching),
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
