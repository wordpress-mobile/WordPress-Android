package org.wordpress.android.fluxc.model

sealed class LocalOrRemoteId {
    class LocalId(val value: Int) : LocalOrRemoteId()
    class RemoteId(val value: Long) : LocalOrRemoteId()
}
