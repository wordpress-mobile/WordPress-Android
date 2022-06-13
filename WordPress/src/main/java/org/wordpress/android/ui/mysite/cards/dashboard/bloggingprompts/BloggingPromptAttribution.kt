package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

enum class BloggingPromptAttribution {
    NO_ATTRIBUTION,
    DAY_ONE;

    companion object {
        fun fromString(value: String): BloggingPromptAttribution = when (value) {
            "dayone" -> DAY_ONE

            else -> NO_ATTRIBUTION
        }
    }
}
