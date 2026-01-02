package org.wordpress.android.posttypes

/**
 * Represents author filter options for post lists.
 */
enum class AuthorFilter(val displayName: String) {
    EVERYONE("Everyone"),
    ME("Me");

    companion object {
        val defaultValue = EVERYONE
    }
}
