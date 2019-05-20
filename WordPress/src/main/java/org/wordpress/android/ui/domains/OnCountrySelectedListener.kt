package org.wordpress.android.ui.domains

import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry

interface OnCountrySelectedListener {
    fun OnCountrySelected(country: SupportedDomainCountry)
}
