package org.wordpress.android.ui.mysite.cards.dashboard.pages

// this class represents the type of post card that will be displayed

enum class PagesCardType(val id: Int) {
    NO_PAGES(0),
    ONLY_ONE_PAGE(1),
    MAX_PAGES(2)
}