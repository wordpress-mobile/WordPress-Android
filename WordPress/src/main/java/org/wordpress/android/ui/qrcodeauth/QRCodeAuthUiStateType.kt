package org.wordpress.android.ui.qrcodeauth

enum class QRCodeAuthUiStateType(val label: String) {
    ERROR("error"),
    CONTENT("content"),
    LOADING("loading"),
    SCANNING("scanning"),
    VALIDATED("validated"),
    AUTHENTICATING("authenticating"),
    DONE("done"),
    INVALID_DATA("invalid_data"),
    AUTHENTICATION_FAILED("authentication_failed"),
    EXPIRED_TOKEN("expired_token"),
    TIMEOUT("timeout"),
    NO_INTERNET("no_internet");

    override fun toString() = label

    companion object {
        @JvmStatic
        fun fromString(strSource: String?): QRCodeAuthUiStateType? {
            if (strSource != null) {
                for (source in entries) {
                    if (source.name.equals(strSource, ignoreCase = true)) {
                        return source
                    }
                }
            }
            return null
        }
    }
}
