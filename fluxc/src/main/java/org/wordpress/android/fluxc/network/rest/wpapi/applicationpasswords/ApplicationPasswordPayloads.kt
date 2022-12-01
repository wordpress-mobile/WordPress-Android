package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

data class ApplicationPasswordCreationPayload(
    val password: String
) : Payload<BaseNetworkError>() {
    constructor(error: BaseNetworkError) : this("") {
        this.error = error
    }
}

data class ApplicationPasswordDeletionPayload(
    val isDeleted: Boolean
) : Payload<BaseNetworkError>() {
    constructor(error: BaseNetworkError) : this(false) {
        this.error = error
    }
}