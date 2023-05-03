@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.prefs

import android.os.Bundle
import android.preference.PreferenceFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope

/**
 * LifecycleOwner is a single method interface that denotes that the class has a Lifecycle.
 * android.preference.PreferenceFragment doesn't implement android.app.Fragment.LifecycleOwner interface.
 * Fragments and Activities in Support Library 26.1.0 and later already implement the LifecycleOwner interface.
 * Until we migrate to androidx Preference Library, we can use this class instead of deprecated PreferenceFragment,
 * which supports the use of lifecycleCoroutineScope for observing Live data or Flows.
 * https://developer.android.com/topic/libraries/architecture/lifecycle#implementing-lco
 */
@Suppress("DEPRECATION")
open class PreferenceFragmentLifeCycleOwner : PreferenceFragment(), LifecycleOwner {
    @Suppress("LeakingThis") private val lifecycleRegistry = LifecycleRegistry(this)

    val lifecycleScope: LifecycleCoroutineScope
        get() = lifecycle.coroutineScope

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(ON_START)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onPause() {
        super.onPause()
        lifecycleRegistry.handleLifecycleEvent(ON_PAUSE)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onStop() {
        super.onStop()
        lifecycleRegistry.handleLifecycleEvent(ON_STOP)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(ON_DESTROY)
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
}
