package org.wordpress.android.fluxc.model.stats.time

data class PostAndPageViewsModel(val views: List<ViewsModel>, val hasMore: Boolean) {
    data class ViewsModel(
        val id: Long,
        val title: String,
        val views: Int,
        val type: ViewsType,
        val url: String
    )

    enum class ViewsType {
        POST,
        PAGE,
        HOMEPAGE,
        OTHER
    }
}
