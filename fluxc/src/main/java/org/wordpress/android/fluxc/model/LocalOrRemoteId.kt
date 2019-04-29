package org.wordpress.android.fluxc.model

sealed class LocalOrRemoteId {
    data class LocalId(val value: Int) : LocalOrRemoteId()
    data class RemoteId(val value: Long) : LocalOrRemoteId()
}
