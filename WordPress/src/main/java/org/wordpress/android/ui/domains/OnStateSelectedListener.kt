package org.wordpress.android.ui.domains

import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse

interface OnStateSelectedListener {
    fun onStateSelected(state: SupportedStateResponse)
}
