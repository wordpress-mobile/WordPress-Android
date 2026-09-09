package org.wordpress.android.ui.domains

import uniffi.wp_api.SupportedCountry

/**
 * Builds a [SupportedCountry] for tests. Only the code and the name are
 * exposed, because that is all the registration form reads; the tax and postal
 * code fields are given false or null.
 */
fun testSupportedCountry(
    code: String,
    name: String,
) = SupportedCountry(
    code = code,
    name = name,
    hasPostalCodes = false,
    vatSupported = false,
    taxNeedsCity = false,
    taxNeedsSubdivision = false,
    taxName = null,
)
