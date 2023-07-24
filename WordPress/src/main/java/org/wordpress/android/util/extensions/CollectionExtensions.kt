package org.wordpress.android.util.extensions

fun <T> Collection<T>.doesNotContain(element: T): Boolean = !contains(element)
