package org.wordpress.android.fluxc.model.blaze

data class BlazeTargetingParameters(
    val locations: List<BlazeTargetingLocation>? = null,
    val languages: List<BlazeTargetingLanguage>? = null,
    val devices: List<BlazeTargetingDevice>? = null,
    val topics: List<BlazeTargetingTopic>? = null
)
