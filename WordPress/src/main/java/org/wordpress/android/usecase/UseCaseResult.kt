package org.wordpress.android.usecase

sealed class UseCaseResult<USE_CASE_TYPE, out ERROR, out DATA>(val type: USE_CASE_TYPE) {
    class Success<USE_CASE_TYPE, DATA>(
        type: USE_CASE_TYPE,
        val data: DATA
    ) : UseCaseResult<USE_CASE_TYPE, Nothing, DATA>(type)

    class Failure<USE_CASE_TYPE, ERROR, DATA>(type: USE_CASE_TYPE, val error: ERROR, val cachedData: DATA) :
        UseCaseResult<USE_CASE_TYPE, ERROR, DATA>(type)

    class Loading<USE_CASE_TYPE>(type: USE_CASE_TYPE) : UseCaseResult<USE_CASE_TYPE, Nothing, Nothing>(type)
}
