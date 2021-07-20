package org.wordpress.android.usecase

sealed class UseCaseResult<out T> {
    data class Success<T>(val data: T) : UseCaseResult<T>()
    data class Failure<ERROR, T>(val error: ERROR, val cachedData: T) : UseCaseResult<T>()
    object Loading : UseCaseResult<Nothing>()
}
