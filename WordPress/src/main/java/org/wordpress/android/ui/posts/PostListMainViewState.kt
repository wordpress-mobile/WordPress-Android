package org.wordpress.android.ui.posts

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

sealed class AuthorFilterListItemUIState(
    val id: Long,
    val text: UiString,
    open val isSelected: Boolean
) {
    data class Everyone(override val isSelected: Boolean, @DrawableRes val imageRes: Int) :
        AuthorFilterListItemUIState(
            id = EVERYONE.id,
            text = UiStringRes(R.string.everyone),
            isSelected = isSelected
        )

    data class Me(val avatarUrl: String?, override val isSelected: Boolean) :
        AuthorFilterListItemUIState(
            id = ME.id,
            text = UiStringRes(R.string.me),
            isSelected = isSelected
        )
}

fun getAuthorFilterItems(
    selection: AuthorFilterSelection,
    avatarUrl: String?
): List<AuthorFilterListItemUIState> {
    return AuthorFilterSelection.values().map { value ->
        when (value) {
            ME -> AuthorFilterListItemUIState.Me(avatarUrl, selection == value)
            EVERYONE -> AuthorFilterListItemUIState.Everyone(
                selection == value,
                R.drawable.ic_people_white_24dp
            )
        }
    }
}
