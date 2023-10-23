package org.wordpress.android.ui.prefs.privacy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.geo.WpComGeoRestClient
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GeoRepository @Inject constructor(
    @Named(IO_THREAD) val ioDispatcher: CoroutineDispatcher,
    private val wpComGeoRestClient: WpComGeoRestClient,
) {
    private suspend fun fetchCountryCode() = withContext(ioDispatcher) {
        wpComGeoRestClient.fetchCountryCode().map { it.orEmpty() }
    }

    suspend fun isGdprComplianceRequired() = fetchCountryCode().fold(
        onSuccess = { countryCode ->
            countryCode.uppercase() in GDPR_COUNTRY_CODES
        },
        onFailure = {
            false
        }
    )

    companion object {
        @Suppress("ForbiddenComment")
        // TODO: Consider extracting this to a common lib (i.e. WordPress-Utils-Android)
        private val GDPR_COUNTRY_CODES = listOf(
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
            // Single Market Countries that GDPR applies to
            "CH", // Switzerland
            "GB", // United Kingdom
            "IS", // Iceland
            "LI", // Liechtenstein
            "NO", // Norway
        )
    }
}
