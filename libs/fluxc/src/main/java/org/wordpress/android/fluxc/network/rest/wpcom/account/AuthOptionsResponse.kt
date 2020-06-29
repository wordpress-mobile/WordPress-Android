package org.wordpress.android.fluxc.network.rest.wpcom.account

import org.wordpress.android.fluxc.network.Response

class AuthOptionsResponse : Response {
    var passwordless: Boolean? = null
    var email_verified: Boolean? = null
}
