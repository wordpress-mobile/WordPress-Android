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
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.core.os.ParcelCompat
import java.io.Serializable

/* ON BACK PRESSED */

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

/* INTENT */

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? =
    IntentCompat.getParcelableExtra(
        this,
        key,
        T::class.java
    )

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

/* BUNDLE */

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? =
    BundleCompat.getParcelable(
        this,
        key,
        T::class.java
    )

inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? =
    BundleCompat.getParcelableArrayList(
        this,
        key,
        T::class.java
    )

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

/* PARCEL */

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(loader: ClassLoader?): T? =
    ParcelCompat.readParcelable(
        this,
        loader,
        T::class.java
    )

inline fun <reified T> Parcel.readListCompat(outVal: MutableList<T?>, loader: ClassLoader?) {
    ParcelCompat.readList(
        this,
        outVal,
        loader,
        T::class.java
    )
}

/* PACKAGE MANAGER */

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
