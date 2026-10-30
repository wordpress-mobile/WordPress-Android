package org.wordpress.android.ui.reader.views.compose

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Compose content whose composition lasts as long as [fragment] rather than its view, for fragments that host
 * other fragments with AndroidFragment.
 *
 * WPMainActivity detaches the Reader when another bottom-nav tab is picked, which destroys its view. A composition
 * tied to that view would be disposed, and AndroidFragment would remove its fragments with it, so coming back would
 * rebuild every feed. So the composition uses the fragment as its lifecycle and saved state owner, and [view]
 * returns the same view each time, keeping the hosted fragments the way the old child fragments were kept.
 */
class RetainedComposeView(private val fragment: Fragment, private val content: @Composable () -> Unit) {
    private var view: View? = null

    fun view(): View {
        val view = view ?: MeasureOnLayoutComposeView(fragment.requireContext(), content) {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(fragment))
            setViewTreeLifecycleOwner(fragment)
            setViewTreeSavedStateRegistryOwner(fragment)
        }.also { view = it }
        // normally already removed when the fragment's view was destroyed
        (view.parent as? ViewGroup)?.removeView(view)
        return view
    }
}
