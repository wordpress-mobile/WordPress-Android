package org.wordpress.android.usecase

sealed class UseCaseResult<out T, USE_CASE_TYPE>(val type: USE_CASE_TYPE) {
    class Success<T, USE_CASE_TYPE>(type: USE_CASE_TYPE, val data: T) : UseCaseResult<T, USE_CASE_TYPE>(type)
    class Failure<ERROR, T, USE_CASE_TYPE>(type: USE_CASE_TYPE, val error: ERROR, val cachedData: T) : UseCaseResult<T, USE_CASE_TYPE>(type)
    class Loading<USE_CASE_TYPE>(type: USE_CASE_TYPE) : UseCaseResult<Nothing, USE_CASE_TYPE>(type)
}
