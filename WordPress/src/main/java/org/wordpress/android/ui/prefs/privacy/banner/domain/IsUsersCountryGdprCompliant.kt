package org.wordpress.android.ui.prefs.privacy.banner.domain

import org.wordpress.android.data.LocaleProvider
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class IsUsersCountryGdprCompliant @Inject constructor(
    private val accountStore: AccountStore,
    private val telephonyManagerProvider: TelephonyManagerProvider,
    private val localeProvider: LocaleProvider,
) {
    operator fun invoke(): Boolean {
        val countryCode = let {
            if (accountStore.hasAccessToken()) accountStore.account.userIpCountryCode
            else countryCodeFromPhoneCarrierOrLocale()
        }.uppercase()
        return countryCode in PRIVACY_BANNER_ELIGIBLE_COUNTRY_CODES
    }

    private fun countryCodeFromPhoneCarrierOrLocale() =
        telephonyManagerProvider.getCountryCode().ifEmpty { localeProvider.provide().country.orEmpty() }
}

private val PRIVACY_BANNER_ELIGIBLE_COUNTRY_CODES = listOf(
    // European Member countries
    "AT", // Austria
    "BE", // Belgium
    "BG", // Bulgaria
    "CY", // Cyprus
    "CZ", // Czech Republic
    "DE", // Germany
    "DK", // Denmark
    "EE", // Estonia
    "ES", // Spain
    "FI", // Finland
    "FR", // France
    "GR", // Greece
    "HR", // Croatia
    "HU", // Hungary
    "IE", // Ireland
    "IT", // Italy
    "LT", // Lithuania
    "LU", // Luxembourg
    "LV", // Latvia
    "MT", // Malta
    "NL", // Netherlands
    "PL", // Poland
    "PT", // Portugal
    "RO", // Romania
    "SE", // Sweden
    "SI", // Slovenia
    "SK", // Slovakia
    "GB", // United Kingdom
    // Single Market Countries that GDPR applies to
    "CH", // Switzerland
    "IS", // Iceland
    "LI", // Liechtenstein
    "NO", // Norway
)

