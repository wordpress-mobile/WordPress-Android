package org.wordpress.android.fluxc.utils.extensions

import org.wordpress.android.fluxc.model.SiteModel

/**
 * Returns either the XML-RPC username or the API REST one if available
 */
fun SiteModel.getUserNameProcessed(): String {
    return if (apiRestUsernamePlain.isNullOrEmpty()) {
        username.orEmpty()
    } else {
        apiRestUsernamePlain
    }
}

/**
 * Returns either the XML-RPC password or the API REST one if available
 */
fun SiteModel.getPasswordProcessed(): String {
    return if (apiRestPasswordPlain.isNullOrEmpty()) {
        password.orEmpty()
    } else {
        apiRestPasswordPlain
    }
}

