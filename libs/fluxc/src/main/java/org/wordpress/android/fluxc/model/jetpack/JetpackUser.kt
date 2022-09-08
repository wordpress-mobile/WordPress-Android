package org.wordpress.android.fluxc.model.jetpack

data class JetpackUser(
    val isConnected: Boolean,
    val isMaster: Boolean,
    val username: String,
    val wpcomId: Long,
    val wpcomUsername: String,
    val wpcomEmail: String
)
