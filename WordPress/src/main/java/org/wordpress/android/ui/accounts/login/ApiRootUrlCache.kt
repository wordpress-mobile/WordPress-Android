package org.wordpress.android.ui.accounts.login

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache to store and retrieve the ApiRootUrl between the login discovery process and the final store step
 * This cache is necessary because between those states we are interrupting the code flow by calling external intents
 * so we need a place to safely store the data
 */
@Singleton
class ApiRootUrlCache @Inject constructor() {
    private val cache = mutableMapOf<String, String>()

    fun put(key: String, value: String) {
        if (key.isEmpty() || value.isEmpty()) {
            return
        }
        cache[key] = value
    }

    fun get(key: String): String? {
        return cache[key]
    }
}
