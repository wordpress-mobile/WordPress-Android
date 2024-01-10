package org.wordpress.android.ui.prefs.notifications

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import org.wordpress.android.R

/** Child Notification fragments should inherit from this class in order to make navigation consistent.*/
abstract class ChildNotificationSettingsFragment: PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainSwitchVisibility(View.GONE)
    }

    override fun onDestroy() {
        super.onDestroy()
        setMainSwitchVisibility(View.VISIBLE)
    }

    private fun setMainSwitchVisibility(visibility: Int) {
        with(requireActivity()) {
            val mainSwitchToolBarView = findViewById<PrefMainSwitchToolbarView>(R.id.main_switch)
            val rootView = findViewById<AppBarLayout>(R.id.app_bar_layout)
            val transition: Transition = Slide(Gravity.TOP)
            transition.duration = 200
            transition.addTarget(R.id.main_switch)

            TransitionManager.beginDelayedTransition(rootView, transition)
            mainSwitchToolBarView.visibility = visibility
        }
    }
}
