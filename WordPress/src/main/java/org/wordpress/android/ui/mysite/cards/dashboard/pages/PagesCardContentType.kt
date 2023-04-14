package org.wordpress.android.ui.mysite.cards.dashboard.pages

// this class represents the type of pages card that will be displayed
enum class PagesCardContentType(val status: String) {
    DRAFT("draft"),
    PUBLISHED("published"),
    SCHEDULED("scheduled")
}
