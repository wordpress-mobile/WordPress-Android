package org.wordpress.android.ui.posts

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

data class PostListMainViewState(
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
    @ColorRes open val dropDownBackground: Int
) {
    data class Everyone(@ColorRes override val dropDownBackground: Int, @DrawableRes val imageRes: Int) :
            AuthorFilterListItemUIState(
                    id = AuthorFilterSelection.EVERYONE.id,
                    text = UiStringRes(R.string.post_list_author_everyone),
                    dropDownBackground = dropDownBackground
            )

    data class Me(val avatarUrl: String?, @ColorRes override val dropDownBackground: Int) :
            AuthorFilterListItemUIState(
                    id = AuthorFilterSelection.ME.id,
                    text = UiStringRes(R.string.post_list_author_me),
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
            EVERYONE -> AuthorFilterListItemUIState.Everyone(
                    backgroundColorRes,
                    R.drawable.bg_oval_neutral_30_multiple_users_white_40dp
            )
        }
    }
}
