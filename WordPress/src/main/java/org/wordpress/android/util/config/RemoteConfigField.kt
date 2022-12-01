package org.wordpress.android.util.config

open class RemoteConfigField<T>(
    val defaultValue: String,
    val remoteField: T
)
