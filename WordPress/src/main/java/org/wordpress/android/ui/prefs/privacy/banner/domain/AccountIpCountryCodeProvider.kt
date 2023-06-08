package org.wordpress.android.ui.prefs.privacy.banner.domain

import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Gets the country code from the account IP address if available or null
 */
class AccountIpCountryCodeProvider @Inject constructor(
    private val accountStore: AccountStore,
) : ReadOnlyProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = accountStore.account.userIpCountryCode
        .takeIf { accountStore.hasAccessToken() }
}
