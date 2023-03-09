package org.wordpress.android.util.extensions

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher

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
