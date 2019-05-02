package org.wordpress.android.ui.posts

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.util.image.ImageType.MULTI_USER_AVATAR_GREY_BACKGROUND

class PostListMainViewState(
    val isFabVisible: Boolean,
    val isAuthorFilterVisible: Boolean,
    val authorFilterSelection: AuthorFilterSelection,
    val authorFilterItems: List<AuthorFilterListItemUIState>
)

sealed class PostListViewLayoutTypeMenuUiState(@DrawableRes val iconRes: Int, val title: UiString) {
    object StandardViewLayoutTypeMenuUiState : PostListViewLayoutTypeMenuUiState(
            iconRes = R.drawable.ic_view_post_compact_white_24dp,
            title = UiStringRes(R.string.post_list_toggle_item_layout_list_view)
    )

    object CompactViewLayoutTypeMenuUiState : PostListViewLayoutTypeMenuUiState(
            iconRes = R.drawable.ic_view_post_full_white_24dp,
            title = UiStringRes(R.string.post_list_toggle_item_layout_cards_view)
    )
}

sealed class AuthorFilterListItemUIState(
    val id: Long,
    val text: UiString,
    val avatarUrl: String?,
    val imageType: ImageType,
    @ColorRes val dropDownBackground: Int
) {
    class Everyone(@ColorRes dropDownBackground: Int) : AuthorFilterListItemUIState(
            id = AuthorFilterSelection.EVERYONE.id,
            text = UiStringRes(R.string.post_list_author_everyone),
            avatarUrl = null,
            imageType = MULTI_USER_AVATAR_GREY_BACKGROUND,
            dropDownBackground = dropDownBackground
    )

    class Me(avatarUrl: String?, @ColorRes dropDownBackground: Int) : AuthorFilterListItemUIState(
            id = AuthorFilterSelection.ME.id,
            text = UiStringRes(R.string.post_list_author_me),
            avatarUrl = avatarUrl,
            imageType = AVATAR_WITH_BACKGROUND,
            dropDownBackground = dropDownBackground
    )
}

fun getAuthorFilterItems(
    selection: AuthorFilterSelection,
    avatarUrl: String?
): List<AuthorFilterListItemUIState> {
    return AuthorFilterSelection.values().map { value ->
        @ColorRes val backgroundColorRes: Int =
                if (selection == value) R.color.grey_lighten_30_translucent_50
                else R.color.transparent

        when (value) {
            ME -> AuthorFilterListItemUIState.Me(avatarUrl, backgroundColorRes)
            EVERYONE -> AuthorFilterListItemUIState.Everyone(backgroundColorRes)
        }
    }
}
