package org.wordpress.android.fluxc.model.blaze

data class BlazeTargetingParameters(
    val locations: List<String>? = null,
    val languages: List<String>? = null,
    val devices: List<String>? = null,
    val topics: List<String>? = null
)
