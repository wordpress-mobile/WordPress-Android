package org.wordpress.android.fluxc.model.scan

import java.util.Date

data class ScanStateModel(
    val state: State,
    val reason: String?,
    val threats: List<ThreatModel>?,
    val credentials: List<Credentials>?,
    val hasCloud: Boolean,
    val mostRecentStatus: ScanProgressStatus?,
    val currentStatus: ScanProgressStatus?
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
        val host: String?,
        val port: Int?,
        val user: String?,
        val path: String?,
        val stillValid: Boolean
    )

    data class ScanProgressStatus(
        val startDate: Date,
        val duration: Int = 0,
        val progress: Int,
        val error: Boolean = false,
        val isInitial: Boolean
    )
}
