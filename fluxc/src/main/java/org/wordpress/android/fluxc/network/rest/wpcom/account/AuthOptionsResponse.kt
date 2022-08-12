package org.wordpress.android.fluxc.network.rest.wpcom.account

import org.wordpress.android.fluxc.network.Response

@Suppress("VariableNaming")
class AuthOptionsResponse : Response {
    var passwordless: Boolean? = null
    var email_verified: Boolean? = null
}
