package org.wordpress.android.ui.prefs.notifications

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_DISABLED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_ENABLED
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.notifications.NotificationEvents.NotificationsSettingsStatusChanged
import org.wordpress.android.ui.prefs.notifications.PrefMainSwitchToolbarView.MainSwitchToolbarListener
import org.wordpress.android.ui.prefs.notifications.usecase.UpdateNotificationSettingsUseCase
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class NotificationsSettingsActivity : LocaleAwareActivity(), MainSwitchToolbarListener {
    @Inject
    lateinit var updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase

    @Inject
    @Named(APPLICATION_SCOPE)
    lateinit var applicationScope: CoroutineScope

    private lateinit var messageTextView: TextView
    private lateinit var messageContainer: View

    private lateinit var fragmentContainer: View

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.notifications_settings_activity)
        fragmentContainer = findViewById(id.fragment_container)

        setUpPrimaryToolbar()

        setUpMainSwitch()

        if (savedInstanceState == null) {
            @Suppress("DEPRECATION")
            fragmentManager.beginTransaction()
                .add(id.fragment_container, NotificationsSettingsFragment())
                .commit()
        }

        messageContainer = findViewById(id.notifications_settings_message_container)
        messageTextView = findViewById(id.notifications_settings_message)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: NotificationsSettingsStatusChanged) {
        if (TextUtils.isEmpty(event.message)) {
            messageContainer.visibility = View.GONE
        } else {
            messageContainer.visibility = View.VISIBLE
            messageTextView.text = event.message
        }
    }

    /**
     * Set up primary toolbar for navigation and search
     */
    private fun setUpPrimaryToolbar() {
        val toolbar = findViewById<Toolbar>(id.toolbar_with_search)

        toolbar?.let { setSupportActionBar(it) }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    /**
     * Sets up main switch to disable/enable all notification settings
     */
    private fun setUpMainSwitch() {
        val mainSwitchToolBarView = findViewById<PrefMainSwitchToolbarView>(id.main_switch)
        mainSwitchToolBarView.setMainSwitchToolbarListener(this)

        // Set main switch state from shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@NotificationsSettingsActivity)
        val isMainChecked = sharedPreferences.getBoolean(getString(string.wp_pref_notifications_main), true)
        mainSwitchToolBarView.loadInitialState(isMainChecked)
        hideDisabledView(isMainChecked)
    }

    override fun onMainSwitchCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        applicationScope.launch { updateNotificationSettingsUseCase.updateNotificationSettings(isChecked) }

        hideDisabledView(isChecked)

        if (isChecked) {
            AnalyticsTracker.track(NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_ENABLED)
        } else {
            AnalyticsTracker.track(NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_DISABLED)
        }
    }

    /**
     * Hide view when Notification Settings are disabled by toggling the main switch off.
     *
     * @param isMainChecked TRUE to hide disabled view, FALSE to show disabled view
     */
    private fun hideDisabledView(isMainChecked: Boolean) {
        val notificationsDisabledView = findViewById<LinearLayout>(id.notification_settings_disabled_view)
        notificationsDisabledView.visibility = if (isMainChecked) View.INVISIBLE else View.VISIBLE
        fragmentContainer.visibility = if (isMainChecked) View.VISIBLE else View.GONE
    }
}
