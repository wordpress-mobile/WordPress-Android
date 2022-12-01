package org.wordpress.android.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class RemoteConfig(val remoteField: String, val defaultValue: String)
