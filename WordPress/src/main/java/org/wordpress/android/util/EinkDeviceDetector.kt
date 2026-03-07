package org.wordpress.android.util

import android.os.Build

/**
 * Detects whether the current device is an e-ink device by checking
 * Build properties against a list of known e-ink manufacturers, brands,
 * and model patterns. The device list is sourced from KOReader's
 * DeviceInfo.kt (https://github.com/koreader/android-luajit-launcher).
 */
object EinkDeviceDetector {
    private val EINK_MANUFACTURERS = setOf(
        "onyx",
        "boyue",
        "pocketbook",
        "bigme",
        "hanvon",
        "hyread",
        "dasung",
        "topjoy",
        "moan",
        "artatech",  // InkBook
        "energy sistem",
        "crema",
        "tolino",
        "viwoods",
        "supernote",
        "ratta",  // Supernote parent company
        "meebook",
        "haoqing",  // Meebook manufacturer
        "barnesandnoble",
        "remarkable",
    )

    private val EINK_BRANDS = setOf(
        "onyx",
        "boox",
        "boyue",
        "pocketbook",
        "kobo",
        "bigme",
        "hanvon",
        "hyread",
        "dasung",
        "topjoy",
        "moan",
        "likebook",
        "tolino",
        "viwoods",
        "aipaper reader",
        "supernote",
        "meebook",
        "inkbook",
        "nook",
    )

    // Model patterns for dual-use manufacturers that also make
    // non-e-ink devices. Paired with their manufacturer to
    // prevent false positives (e.g. Huawei Nova matching "nova").
    //
    // Note: Amazon Kindle e-readers run custom Linux, not Android,
    // so they can't install this app. The "kindle" pattern here is
    // defensive — Fire tablets (Android) use "KF*" model codes and
    // won't match.
    private val EINK_MODELS_BY_MANUFACTURER = mapOf(
        "amazon" to setOf("kindle"),
        "hisense" to setOf("a5pro", "a5 pro", "a7cc"),
        "xiaomi" to setOf("xiaomi_reader"),
    )

    fun isEinkDevice(): Boolean = isEinkDevice(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
    )

    internal fun isEinkDevice(
        manufacturer: String,
        brand: String,
        model: String,
    ): Boolean {
        val mfr = manufacturer.lowercase().trim()
        val br = brand.lowercase().trim()
        val mdl = model.lowercase().trim()

        if (mfr in EINK_MANUFACTURERS) return true
        if (br in EINK_BRANDS) return true

        val patterns = EINK_MODELS_BY_MANUFACTURER[mfr]
            ?: return false
        return patterns.any { mdl.contains(it) }
    }
}
