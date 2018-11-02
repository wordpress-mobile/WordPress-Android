package org.wordpress.android.fluxc.model.stats

data class VisitsModel(
    val period: String,
    val views: Int,
    val visitors: Int,
    val likes: Int,
    val reblogs: Int,
    val comments: Int,
    val posts: Int
)
