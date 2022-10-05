package org.wordpress.android.util.extensions

val Int.isOdd get() = this % 2 != 0
val Float.isNegative get () = this < 0
