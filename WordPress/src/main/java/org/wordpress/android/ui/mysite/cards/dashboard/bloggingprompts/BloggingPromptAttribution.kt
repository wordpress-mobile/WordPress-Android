package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

enum class BloggingPromptAttribution {
    NO_ATTRIBUTION,
    DAY_ONE,
    BLOGANUARY;

    companion object {
        fun fromString(value: String): BloggingPromptAttribution = when (value) {
            "dayone" -> DAY_ONE
            "bloganuary" -> BLOGANUARY
            else -> NO_ATTRIBUTION
        }
    }
}
