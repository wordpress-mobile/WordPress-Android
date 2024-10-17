package org.wordpress.android.fluxc.model.stats

data class PublicizeModel(val services: List<Service>, val hasMore: Boolean) {
    data class Service(val name: String, val followers: Int)
}
