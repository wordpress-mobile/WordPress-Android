package org.wordpress.android.fluxc.model.scan

data class ThreatModel(
    val id: Int,
    val signature: String,
    val description: String,
    val status: String,
    val fixable: Fixable,
    val extension: Extension,
    val firstDetected: String
) {
    data class Fixable(
        val fixer: String,
        val target: String
    )

    data class Extension(
        val type: String,
        val slug: String,
        val name: String,
        val version: String,
        val isPremium: Boolean
    )
}
