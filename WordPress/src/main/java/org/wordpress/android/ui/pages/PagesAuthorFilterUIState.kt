package org.wordpress.android.ui.pages

import org.wordpress.android.ui.posts.AuthorFilterListItemUIState
import org.wordpress.android.ui.posts.AuthorFilterSelection

data class PagesAuthorFilterUIState(
    val isAuthorFilterVisible: Boolean,
    val authorFilterSelection: AuthorFilterSelection,
    val authorFilterItems: List<AuthorFilterListItemUIState>
)
