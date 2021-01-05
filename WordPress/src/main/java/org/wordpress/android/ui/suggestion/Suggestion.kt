package org.wordpress.android.ui.suggestion

import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.models.UserSuggestion

data class Suggestion(val avatarUrl: String, val value: String, val displayValue: String) {
    companion object {
        fun fromUserSuggestions(userSuggestions: List<UserSuggestion>): List<Suggestion> =
                userSuggestions.map {
                    Suggestion(it.imageUrl, it.userLogin, it.displayName)
                }

        fun fromXpost(xpost: XPostSiteModel): Suggestion = Suggestion(xpost.blavatar, xpost.subdomain, xpost.title)
    }
}
