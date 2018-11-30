package org.wordpress.android.fluxc.model.stats

data class TagsModel(val tags: List<TagModel>, val hasMore: Boolean) {
    class TagModel(val items: List<Item>, val views: Long) {
        data class Item(val name: String, val type: String, val link: String)
    }
}
