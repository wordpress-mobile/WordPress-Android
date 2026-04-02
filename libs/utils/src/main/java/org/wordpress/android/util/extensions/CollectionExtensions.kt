package org.wordpress.android.util.extensions

fun <T> Collection<T>.doesNotContain(element: T): Boolean = !contains(element)

fun <T> Collection<T>.hasOneElement(): Boolean = size == 1

fun <T> Collection<T>.indexOrNull(predicate: (T) -> Boolean): Int? =
    indexOfFirst(predicate).let {
        if (it == -1) null else it
    }
