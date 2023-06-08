package org.wordpress.android.ui.prefs.privacy.banner.domain

import org.wordpress.android.data.LocaleProvider
import javax.inject.Inject

class IsUsersCountryGdprCompliant @Inject constructor(
    localeProvider: LocaleProvider,
    carrierCountryCodeProvider: CarrierCountryCodeProvider,
    accountIpCountryCodeProvider: AccountIpCountryCodeProvider,
) {
    operator fun invoke(): Boolean {
        val countryCode = accountIpCountryCode ?: carrierCountryCode ?: deviceLocale.country.orEmpty()
        return countryCode in PRIVACY_BANNER_ELIGIBLE_COUNTRY_CODES
    }

    private val accountIpCountryCode by accountIpCountryCodeProvider
    private val carrierCountryCode by carrierCountryCodeProvider
    private val deviceLocale by localeProvider
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

