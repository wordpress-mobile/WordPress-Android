package org.wordpress.android.imageeditor.utils

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat

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

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(key: String): ArrayList<T>? =
    IntentCompat.getParcelableArrayListExtra(
        this,
        key,
        T::class.java
    )

/* BUNDLE */

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? =
    BundleCompat.getParcelable(
        this,
        key,
        T::class.java
    )
