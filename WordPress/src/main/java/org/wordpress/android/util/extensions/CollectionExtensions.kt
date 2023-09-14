package org.wordpress.android.util.extensions

fun <T> Collection<T>.doesNotContain(element: T): Boolean = !contains(element)

fun <T> Collection<T>.hasOneElement(): Boolean = size == 1
