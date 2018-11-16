package org.wordpress.android.fluxc.model.stats

data class PublicizeModel(val services: List<Service>) {
    data class Service(val name: String, val followers: Int)
}
