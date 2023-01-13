package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

internal data class ApplicationPasswordCredentials(
    val userName: String,
    val password: String,
    val uuid: String
)
