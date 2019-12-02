package org.wordpress.android.widgets

import android.view.View
import dagger.Reusable
import javax.inject.Inject

/**
 * Injectable wrapper around WPSnackbar.
 *
 * WPSnackbar interfaces are consisted of static methods, which
 * makes the client code difficult to test/mock. Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class WPSnackbarWrapper @Inject constructor() {
    fun make(view: View, text: CharSequence, duration: Int): WPSnackbar = WPSnackbar.make(view, text, duration)
}
