package org.wordpress.android.fluxc.model.stats.time

data class PostAndPageViewsModel(val views: List<ViewsModel>, val hasMore: Boolean) {
    data class ViewsModel(
        val title: String,
        val views: Int,
        val type: ViewsType
    )
    enum class ViewsType {
        POST,
        PAGE
    }
}
