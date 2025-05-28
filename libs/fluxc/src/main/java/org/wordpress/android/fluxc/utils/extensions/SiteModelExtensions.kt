package org.wordpress.android.fluxc.utils.extensions

import org.wordpress.android.fluxc.model.SiteModel

/**
 * Returns either the XML-RPC username or the API REST one if available
 */
fun SiteModel.getUserNameProcessed(): String {
    return if (apiRestUsername.isNullOrEmpty()) {
        username
    } else {
        apiRestUsername
    }
}

/**
 * Returns either the XML-RPC password or the API REST one if available
 */
fun SiteModel.getPasswordProcessed(): String {
    return if (apiRestPassword.isNullOrEmpty()) {
        password
    } else {
        apiRestPassword
    }
}

