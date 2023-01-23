package org.wordpress.android.ui.recommend

sealed class RecommendAppState {
    object FetchingApi : RecommendAppState()
    data class ApiFetchedResult(
        val error: String? = null,
        val message: String,
        val link: String
    ) : RecommendAppState() {
        constructor(error: String) : this(error, "", "")

        fun isError() = error != null
    }
}
