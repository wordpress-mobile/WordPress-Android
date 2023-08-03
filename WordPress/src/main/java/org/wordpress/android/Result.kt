package org.wordpress.android

sealed class Result<out L, out R> {
    class Failure<out L>(val value: L) : Result<L, Nothing>()
    class Success<out R>(val value: R) : Result<Nothing, R>()
}
