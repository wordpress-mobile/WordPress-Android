package org.wordpress.android.util.publicdata

import org.wordpress.android.BuildConfig
import org.wordpress.android.util.publicdata.JetpackPublicData.PackageName.Jalapeno
import org.wordpress.android.util.publicdata.JetpackPublicData.PackageName.Vanilla
import org.wordpress.android.util.publicdata.JetpackPublicData.PackageName.Wasabi
import org.wordpress.android.util.publicdata.JetpackPublicData.PublicKeyHash.Debug
import org.wordpress.android.util.publicdata.JetpackPublicData.PublicKeyHash.Release
import javax.inject.Inject

class JetpackPublicData @Inject constructor() {
    private sealed class PublicKeyHash(val type: String, val value: String) {
        object Release : PublicKeyHash("release", "f2d7acc12614750009514a0932bf0b0aa9c11829a66e862ce4572bced344e76e")

        object Debug : PublicKeyHash("debug", "60fca11c59c6933610146f40a1296250abff640dc5da2b85fc8e5aa411dd17d6")
    }

    fun currentPublicKeyHash() = when (BuildConfig.BUILD_TYPE) {
        Release.type -> Release.value
        Debug.type -> Debug.value
        else -> throw IllegalArgumentException("Failed to get Jetpack public key hash: build type not found")
    }

    private sealed class PackageName(val type: String, val value: String) {
        object Jalapeno : PackageName("jalapeno", "com.jetpack.android.prealpha")

        object Vanilla : PackageName("vanilla", "com.jetpack.android")

        object Wasabi : PackageName("wasabi", "com.jetpack.android.beta")
    }

    fun currentPackageId(): String = when (BuildConfig.FLAVOR_buildType) {
        Jalapeno.type -> Jalapeno.value
        Vanilla.type -> Vanilla.value
        Wasabi.type -> Wasabi.value
        else -> throw IllegalArgumentException("Failed to get Jetpack package ID: build flavor not found.")
    }
}
