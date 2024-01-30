package org.wordpress.android.ui.sitemonitor

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment

@Suppress("SwallowedException")
@Composable
internal fun SiteMonitorFragmentContainer(
    modifier: Modifier = Modifier,
    commit: FragmentTransaction.(containerId: Int) -> Unit
) {
    val currentLocalView = LocalView.current
    // Using the current view, check if a parent fragment exists.
    // This will help ensure that the fragment are nested correctly.
    // This assists in saving/restoring the fragments to their proper state
    val parentFragment = remember(currentLocalView) {
        try {
            currentLocalView.findFragment<Fragment>()
        } catch (e: IllegalStateException) {
            null
        }
    }
    val viewId by rememberSaveable { mutableIntStateOf(View.generateViewId()) }
    val container = remember { mutableStateOf<FragmentContainerView?>(null) }
    val viewSection: (Context) -> View = remember(currentLocalView) {
        { context ->
            FragmentContainerView(context)
                .apply { id = viewId }
                .also {
                    val fragmentManager = parentFragment?.childFragmentManager
                        ?: (context as? FragmentActivity)?.supportFragmentManager
                    fragmentManager?.commit { commit(it.id) }
                    container.value = it
                }
        }
    }
    AndroidView(
        modifier = modifier,
        factory = viewSection,
        update = {}
    )

    // Be sure to clean up the fragments when the FragmentContainer is disposed
    val localContext = LocalContext.current
    DisposableEffect(currentLocalView, localContext, container) {
        onDispose {
            val fragmentManager = parentFragment?.childFragmentManager
                ?: (localContext as? FragmentActivity)?.supportFragmentManager
            // Use the FragmentContainerView to find the inflated fragment
            val existingFragment = fragmentManager?.findFragmentById(container.value?.id ?: 0)
            if (existingFragment != null && !fragmentManager.isStateSaved) {
                // A composable has been removed from the hierarchy if the state isn't saved
                fragmentManager.commit {
                    remove(existingFragment)
                }
            }
        }
    }
}
