package org.wordpress.android.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Feature(val remoteField: String, val defaultValue: Boolean = false)
