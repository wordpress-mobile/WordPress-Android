package org.wordpress.android.fluxc.model.scan

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import java.util.Date

data class ScanStateModel(
    val state: State,
    val reason: Reason,
    val threats: List<ThreatModel>? = null,
    val credentials: List<Credentials>? = null,
    val hasCloud: Boolean = false,
    val mostRecentStatus: ScanProgressStatus? = null,
    val currentStatus: ScanProgressStatus? = null,
    val hasValidCredentials: Boolean = false
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

    enum class Reason(val value: String?) {
        MULTISITE_NOT_SUPPORTED("multisite_not_supported"),
        VP_ACTIVE_ON_SITE("vp_active_on_site"),
        NO_REASON(null),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String?): Reason {
                return values().firstOrNull { it.value == value } ?: UNKNOWN
            }
        }
    }
}
