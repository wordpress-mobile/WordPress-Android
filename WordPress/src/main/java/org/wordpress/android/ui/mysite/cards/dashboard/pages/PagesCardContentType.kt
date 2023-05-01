package org.wordpress.android.ui.mysite.cards.dashboard.pages

// this class represents the type of pages card that will be displayed
enum class PagesCardContentType(val status: String) {
    DRAFT("draft"),
    PUBLISH("publish"),
    SCHEDULED("future");

    companion object {
        fun getList(): List<String> {
            return values().map {
                it.toString()
            }
        }

        fun fromString(status: String): PagesCardContentType {
            return values().first { it.status == status }
        }
    }
}
