package org.wordpress.android.fluxc.model.stats

data class TagsModel(val tags: List<TagModel>) {
    sealed class TagModel {
        data class SingleTag(
            val name: String,
            val type: String,
            val link: String,
            val views: Long?
        ) : TagModel()

        data class Category(val tags: List<SingleTag>, val views: Long) : TagModel()
    }
}
