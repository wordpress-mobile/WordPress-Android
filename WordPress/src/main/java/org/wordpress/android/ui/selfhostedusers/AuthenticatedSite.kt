package org.wordpress.android.ui.selfhostedusers

import uniffi.wp_api.ParsedUrl

data class AuthenticatedSite(val name: String, val url: ParsedUrl)
