package org.wordpress.android.ui.reader.views.compose

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Compose content whose composition outlives [fragment]'s view, for fragments that host other fragments with
 * AndroidFragment. The fragment returns [view] from onCreateView and calls [onDestroyView] from onDestroyView.
 *
 * WPMainActivity detaches the Reader when another bottom-nav tab is picked, which destroys its view. A composition
 * tied to that view would be disposed, and AndroidFragment would remove its fragments with it, so coming back would
 * rebuild every feed. So the same view is returned each time, and its composition is only disposed when the fragment
 * is going away.
 *
 * It has to be disposed before the hosted fragments are destroyed, since AndroidFragment saves their state as it's
 * disposed, and a fragment's own ON_DESTROY comes after its children are destroyed. So it's disposed when the view
 * is destroyed by a removal, or when the activity is destroyed, which happens before any of its fragments are.
 */
class RetainedComposeView(private val fragment: Fragment, private val content: @Composable () -> Unit) {
    private var view: View? = null
    private var lifecycleOwner: CompositionLifecycleOwner? = null

    private val activityObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) dispose()
    }

    fun view(): View {
        val view = view ?: create().also { view = it }
        // normally already removed when the fragment's view was destroyed
        (view.parent as? ViewGroup)?.removeView(view)
        return view
    }

    fun onDestroyView() {
        val activity = fragment.activity
        val isGoingAway = activity == null || activity.isFinishing || activity.isChangingConfigurations ||
                generateSequence(fragment) { it.parentFragment }.any { it.isRemoving }
        if (isGoingAway) dispose()
    }

    private fun create(): View {
        val owner = CompositionLifecycleOwner(fragment.lifecycle).also { lifecycleOwner = it }
        fragment.requireActivity().lifecycle.addObserver(activityObserver)
        return MeasureOnLayoutComposeView(fragment.requireContext(), content) {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(owner))
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(fragment)
        }
    }

    private fun dispose() {
        fragment.activity?.lifecycle?.removeObserver(activityObserver)
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        view = null
    }
}

/**
 * Follows [source], but can be destroyed before it is
 */
private class CompositionLifecycleOwner(private val source: Lifecycle) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    private val observer = LifecycleEventObserver { _, event -> registry.handleLifecycleEvent(event) }

    override val lifecycle: Lifecycle get() = registry

    init {
        source.addObserver(observer)
    }

    fun destroy() {
        source.removeObserver(observer)
        if (registry.currentState != Lifecycle.State.INITIALIZED) {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
