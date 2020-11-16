package org.wordpress.android.fluxc.model.scan

import java.util.Date

data class ScanStateModel(
    val state: State,
    val reason: String?,
    val threats: List<ThreatModel>?,
    val credentials: List<Credentials>?,
    val hasCloud: Boolean,
    val scanProgressStatus: ScanProgressStatus?
) {
    enum class State(val value: String) {
        IDLE("idle"),
        SCANNING("scanning"),
        PROVISIONING("provisioning"),
        UNAVAILABLE("unavailable"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String): State? {
                return values().firstOrNull { it.value == value }
            }
        }
    }

    data class Credentials(
        val type: String,
        val role: String,
        val stillValid: Boolean
    )

    data class ScanProgressStatus(
        val startDate: Date,
        val duration: Int,
        val progress: Int,
        val error: Boolean,
        val isInitial: Boolean
    )
}
