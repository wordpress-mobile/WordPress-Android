package org.wordpress.android.fluxc.model.stats

data class PostDetailStatsModel(
    val views: Int = 0,
    val dayViews: List<Day>,
    val weekViews: List<Week>,
    val yearsTotal: List<Year>,
    val yearsAverage: List<Year>
) {
    data class Year(val year: Int, val months: List<Month>, val value: Int)
    data class Month(val month: Int, val count: Int)
    data class Week(val days: List<Day>, val average: Int, val total: Int)
    data class Day(val period: String, val count: Int)
}
