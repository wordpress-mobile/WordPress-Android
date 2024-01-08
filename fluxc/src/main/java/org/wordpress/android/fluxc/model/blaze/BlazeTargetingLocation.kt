package org.wordpress.android.fluxc.model.blaze

data class BlazeTargetingLocation(
    val id: Long,
    val name: String,
    val type: String,
    val parent: BlazeTargetingLocation?
)
