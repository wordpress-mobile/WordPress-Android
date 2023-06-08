package org.wordpress.android.data

interface Provider<T> {
    fun provide(): T
}
