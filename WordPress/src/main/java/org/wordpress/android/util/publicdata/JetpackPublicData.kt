package org.wordpress.android.util.publicdata

import org.wordpress.android.BuildConfig
import org.wordpress.android.util.publicdata.JetpackPublicData.PublicKeyHash.Debug
import org.wordpress.android.util.publicdata.JetpackPublicData.PublicKeyHash.Release
import javax.inject.Inject

class JetpackPublicData @Inject constructor() {
    private sealed class PublicKeyHash(val type: String, val value: String) {
        object Release : PublicKeyHash(
            "release",
            "f2d7acc12614750009514a0932bf0b0aa9c11829a66e862ce4572bced344e76e"
        )

        object Debug : PublicKeyHash(
            "debug",
            "60fca11c59c6933610146f40a1296250abff640dc5da2b85fc8e5aa411dd17d6"
        )
    }

    fun currentPublicKeyHash() = when (BuildConfig.BUILD_TYPE) {
        Release.type -> Release.value
        Debug.type -> Debug.value
        else -> throw IllegalArgumentException(
            "Failed to get Jetpack public key hash: build type not found"
        )
    }

    fun currentPackageId(): String = BuildConfig.APPLICATION_ID
}
