package org.wordpress.android.ui.reader.views.compose

import android.view.View
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment

/**
 * Lets a fragment hosted by AndroidFragment pass its scrolling on to the Compose layout around it. Compose only
 * forwards a hosted view's nested scrolling when that view has nested scrolling enabled, and AndroidFragment's
 * container doesn't, so without this the Reader's top bar never sees its lists scroll.
 */
fun Fragment.enableNestedScrollInterop() {
    (view?.parent as? View)?.let { ViewCompat.setNestedScrollingEnabled(it, true) }
}
