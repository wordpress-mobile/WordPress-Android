package org.wordpress.android.fluxc.model.scan

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import java.util.Date

data class ScanStateModel(
    val state: State,
    val reason: String? = null,
    val threats: List<ThreatModel>? = null,
    val credentials: List<Credentials>? = null,
    val hasCloud: Boolean = false,
    val mostRecentStatus: ScanProgressStatus? = null,
    val currentStatus: ScanProgressStatus? = null
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
        val startDate: Date?,
        val duration: Int = 0,
        val progress: Int = 0,
        val error: Boolean = false,
        val isInitial: Boolean = false
    )
}
