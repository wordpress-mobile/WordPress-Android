package org.wordpress.android.util.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import java.io.Serializable

/**
 * This is a temporary workaround for the issue described here: https://issuetracker.google.com/issues/247982487
 * This function temporary disables the callback to allow the system to handle the back pressed event.
 *
 * TODO: Replace this temporary workaround before enabling predictive back gesture on this project
 *  (android:enableOnBackInvokedCallback="true").
 *
 * Related Issue: https://github.com/wordpress-mobile/WordPress-Android/issues/18053
 */
@Suppress("ForbiddenComment")
fun OnBackPressedDispatcher.onBackPressedCompat(onBackPressedCallback: OnBackPressedCallback) {
    onBackPressedCallback.isEnabled = false
    onBackPressed()
    onBackPressedCallback.isEnabled = true
}

/**
 * TODO: Remove this when stable androidx.core 1.10 is released. Use IntentCompat instead.
 */
@Suppress("ForbiddenComment")
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as T?
    }

/**
 * This is an Android 13 compatibility function that is not included in IntentCompat.
 */
inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(key) as T?
    }

/**
 * TODO: Remove this when stable androidx.core 1.10 is released. Use BundleCompat instead.
 */
@Suppress("ForbiddenComment")
inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }

/**
 * TODO: Remove this when stable androidx.core 1.10 is released. Use BundleCompat instead.
 */
@Suppress("ForbiddenComment")
inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayList(key)
    }

/**
 * This is an Android 13 compatibility function that is not included in BundleCompat.
 */
inline fun <reified T : Serializable?> Bundle.getSerializableCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(key) as T?
    }

/**
 * TODO: Remove this when upgrading to androidx.core 1.9.0. Use ParcelCompat instead.
 */
@Suppress("ForbiddenComment")
inline fun <reified T : Parcelable> Parcel.readParcelableCompat(loader: ClassLoader?): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelable(loader, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        readParcelable(loader)
    }
}

/**
 * TODO: Remove this when upgrading to androidx.core 1.9.0. Use ParcelCompat instead.
 */
@Suppress("ForbiddenComment")
inline fun <reified T> Parcel.readListCompat(outVal: MutableList<T?>, loader: ClassLoader?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readList(outVal, loader, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        readList(outVal, loader)
    }
}

fun PackageManager.getActivityInfoCompat(componentName: ComponentName, flags: Int): ActivityInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getActivityInfo(componentName, PackageManager.ComponentInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getActivityInfo(componentName, flags)
    }

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
    }
