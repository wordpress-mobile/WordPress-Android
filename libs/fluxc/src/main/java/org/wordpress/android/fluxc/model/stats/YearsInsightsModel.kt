package org.wordpress.android.fluxc.model.stats

data class YearsInsightsModel(val years: List<YearInsights>) {
    data class YearInsights(
        val avgComments: Double?,
        val avgImages: Double?,
        val avgLikes: Double?,
        val avgWords: Double?,
        val totalComments: Int,
        val totalImages: Int,
        val totalLikes: Int,
        val totalPosts: Int,
        val totalWords: Int,
        val year: String
    )
}
