package org.wordpress.android.ui.sitecreation.domains

import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import javax.inject.Inject

/**
 * Injectable wrapper around  * Injectable wrapper around PublicSuffixDatabase.
 *
 * PublicSuffixDatabase interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
class PublicSuffixDatabaseWrapper @Inject constructor() {
    fun getEffectiveTldPlusOne(domain: String): String? =
            PublicSuffixDatabase.get().getEffectiveTldPlusOne(domain)
}
