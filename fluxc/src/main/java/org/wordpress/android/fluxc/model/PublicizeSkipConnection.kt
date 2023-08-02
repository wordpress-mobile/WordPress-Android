package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

class PublicizeSkipConnection(
    @SerializedName("id") val id: String = "0",
    // e.g. "_wpas_skip_publicize_12345"
    @SerializedName("key") val key: String? = null,
    @SerializedName("value") var value: String? = null
) {
    fun connectionId(): String =
        key?.replace(METADATA_SKIP_PUBLICIZE_PREFIX, "") ?: ""

    fun isConnectionEnabled(): Boolean = value == VALUE_CONNECTION_ENABLED

    fun updateValue(enabled: Boolean) {
        value = if (enabled) VALUE_CONNECTION_ENABLED else VALUE_CONNECTION_DISABLED
    }

    companion object {
        const val METADATA_SKIP_PUBLICIZE_PREFIX = "_wpas_skip_publicize_"

        fun createNew(connectionId: String, enabled: Boolean) =
            PublicizeSkipConnection(
                key = "${METADATA_SKIP_PUBLICIZE_PREFIX}$connectionId",
            ).apply {
                updateValue(enabled)
            }
    }
}

private const val VALUE_CONNECTION_ENABLED = "0"
private const val VALUE_CONNECTION_DISABLED = "1"
