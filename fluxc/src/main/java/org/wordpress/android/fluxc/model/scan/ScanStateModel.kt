package org.wordpress.android.fluxc.model.scan

data class ScanStateModel(
    val state: State,
    val threats: List<Threat>?,
    val credentials: List<Credentials>?,
    val hasCloud: Boolean,
    val mostRecent: MostRecent?
) {
    enum class State(val value: String) {
        SCANNING("scanning"),
        IDLE("idle"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String): State? {
                return values().firstOrNull { it.value == value }
            }
        }
    }

    data class Threat(
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

    data class Credentials(
        val type: String,
        val role: String,
        val stillValid: Boolean
    )

    data class MostRecent(
        val timestamp: String?,
        val duration: Int,
        val progress: Int,
        val error: Boolean,
        val isInitial: Boolean
    )
}
