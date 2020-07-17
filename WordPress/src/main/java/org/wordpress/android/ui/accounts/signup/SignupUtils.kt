package org.wordpress.android.ui.accounts.signup

import java.util.Locale
import javax.inject.Inject

class SignupUtils
@Inject constructor() {
    /**
     * Create a display name from the email address by taking everything before the "@" symbol,
     * removing all non-letters and non-periods, replacing periods with spaces, and capitalizing
     * the first letter of each word.
     *
     * @return [String] to be the display name
     */
    fun createDisplayNameFromEmail(emailAddress: String): String? {
        val username = emailAddress.split("@").firstOrNull() ?: return null
        val usernameArray = username.replace("[^A-Za-z/.]".toRegex(), "").split(".")
        val builder = StringBuilder()
        for (item in usernameArray) {
            if (item.isNotEmpty()) {
                builder.append(item.substring(0, 1).toUpperCase(Locale.ROOT))
                if (item.length > 1) {
                    builder.append(item.substring(1))
                }
                builder.append(" ")
            }
        }
        val displayName = builder.toString().trim()
        return if (displayName.isNotEmpty()) displayName else null
    }

    /**
     * Create a username from the email address by taking everything before the "@" symbol and
     * removing all non-alphanumeric characters.
     *
     * @return [String] to be the username
     */
    fun createUsernameFromEmail(emailAddress: String): String? {
        return emailAddress.split("@".toRegex())[0]
                .replace("[^A-Za-z0-9]".toRegex(), "")
                .toLowerCase(Locale.ROOT)
    }
}
