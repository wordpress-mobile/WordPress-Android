package org.wordpress.android.ui.prefs.notifications

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.databinding.NotificationsSettingsSwitchBinding
import org.wordpress.android.models.NotificationsSettings
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.extensions.getDrawableFromAttribute

class NotificationsSettingsDialogFragment(
    private val channel: NotificationsSettings.Channel,
    private val type: NotificationsSettings.Type,
    private val blogId: Long = 0,
    private val settings: NotificationsSettings,
    private val onNotificationsSettingsChangedListener:
        NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener,
    private val bloggingRemindersProvider: NotificationsSettingsDialogPreference.BloggingRemindersProvider? = null,
    private val title: String
): DialogFragment(), PrefMainSwitchToolbarView.MainSwitchToolbarListener, DialogInterface.OnClickListener {
    companion object {
        const val TAG = "Notifications_Settings_Dialog_Fragment"
        private const val SETTING_VALUE_ACHIEVEMENT = "achievement"
    }

    private val mUpdatedJson = JSONObject()
    private var mTitleViewWithMainSwitch: ViewGroup? = null

    // view to display when main switch is on
    private var mDisabledView: View? = null

    // view to display when main switch is off
    private var mOptionsView: LinearLayout? = null

    private var mMainSwitchToolbarView: PrefMainSwitchToolbarView? = null
    private val mShouldDisplayMainSwitch: Boolean = settings.shouldDisplayMainSwitch(channel, type)

    private var mSettingsArray = arrayOfNulls<String>(0)
    private var mSettingsValues: Array<String?>? = arrayOfNulls<String?>(0)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        @SuppressLint("InflateParams")
        val layout = requireActivity().layoutInflater.inflate(R.layout.notifications_settings_types_dialog, null)

        val outerView = layout.findViewById<ScrollView>(R.id.outer_view)
        outerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val innerView = LinearLayout(context)
        innerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        innerView.orientation = LinearLayout.VERTICAL
        if (mShouldDisplayMainSwitch) {
            val dividerView = View(context)
            val dividerHeight = context.resources.getDimensionPixelSize(
                R.dimen.notifications_settings_dialog_divider_height
            )
            dividerView.background = context.getDrawableFromAttribute(android.R.attr.listDivider)
            dividerView.layoutParams = ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, dividerHeight)
            innerView.addView(dividerView)
        } else {
            val spacerView = View(context)
            val spacerHeight = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            spacerView.layoutParams = ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, spacerHeight)
            innerView.addView(spacerView)
        }
        mDisabledView = View.inflate(context, R.layout.notifications_tab_disabled_text_layout, null)
        mDisabledView?.layoutParams = ViewGroup.LayoutParams(
            ActionBar.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        mOptionsView = LinearLayout(context)
        mOptionsView?.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        mOptionsView?.orientation = LinearLayout.VERTICAL
        innerView.addView(mDisabledView)
        innerView.addView(mOptionsView)
        outerView.addView(innerView)
        configureLayoutForView(mOptionsView!!)

        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(title)
            setPositiveButton(android.R.string.ok, this@NotificationsSettingsDialogFragment)
            setNegativeButton(R.string.cancel, this@NotificationsSettingsDialogFragment)
            setView(layout)
        }
        if (mShouldDisplayMainSwitch) {
            setupTitleViewWithMainSwitch(outerView)
            if (mTitleViewWithMainSwitch == null)
                AppLog.e(AppLog.T.NOTIFS, "Main switch enabled but layout not set")
            else
                builder.setCustomTitle(mTitleViewWithMainSwitch)
        }
        return builder.create()
    }

    private fun setupTitleViewWithMainSwitch(view: View) {
        when (this.channel) {
            NotificationsSettings.Channel.BLOGS -> if (this.type == NotificationsSettings.Type.TIMELINE) {
                mTitleViewWithMainSwitch = layoutInflater
                    .inflate(R.layout.notifications_tab_for_blog_title_layout, view as ViewGroup, false) as ViewGroup
            }

            NotificationsSettings.Channel.OTHER, NotificationsSettings.Channel.WPCOM -> {}
        }
        if (mTitleViewWithMainSwitch != null) {
            val titleView = mTitleViewWithMainSwitch!!.findViewById<TextView>(R.id.title)
            titleView.text = title
            mMainSwitchToolbarView = mTitleViewWithMainSwitch!!.findViewById(R.id.main_switch)
            mMainSwitchToolbarView!!.setMainSwitchToolbarListener(this)
            mMainSwitchToolbarView!!
                .setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))

            // Main Switch initial state:
            // On: If at least one of the settings options is on
            // Off: If all settings options are off
            val settingsJson = settings.getSettingsJsonForChannelAndType(channel, type, blogId)
            val checkMainSwitch = settings.isAtLeastOneSettingsEnabled(
                settingsJson,
                mSettingsArray,
                mSettingsValues
            )
            mMainSwitchToolbarView!!.loadInitialState(checkMainSwitch)
            hideDisabledView(mMainSwitchToolbarView!!.isMainChecked)
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE && mUpdatedJson.length() > 0) {
            onNotificationsSettingsChangedListener.onSettingsChanged(this.channel,
                this.type, this.blogId, mUpdatedJson)

            // Update the settings json
            val keys: Iterator<*> = mUpdatedJson.keys()
            while (keys.hasNext()) {
                val settingName = keys.next() as String
                settings.updateSettingForChannelAndType(
                    this.channel, this.type, settingName,
                    mUpdatedJson.optBoolean(settingName), this.blogId
                )
            }
        }
    }

    private fun configureLayoutForView(view: LinearLayout): View {
        val settingsJson = settings.getSettingsJsonForChannelAndType(this.channel, this.type, this.blogId)
        var summaryArray = arrayOfNulls<String>(0)
        when (this.channel) {
            NotificationsSettings.Channel.BLOGS -> {
                mSettingsArray = requireContext().resources.getStringArray(R.array.notifications_blog_settings)
                mSettingsValues = requireContext().resources.getStringArray(R.array.notifications_blog_settings_values)
            }

            NotificationsSettings.Channel.OTHER -> {
                mSettingsArray = requireContext().resources.getStringArray(R.array.notifications_other_settings)
                mSettingsValues = requireContext().resources.getStringArray(R.array.notifications_other_settings_values)
            }

            NotificationsSettings.Channel.WPCOM -> {
                mSettingsArray = requireContext().resources.getStringArray(R.array.notifications_wpcom_settings)
                mSettingsValues = requireContext().resources.getStringArray(R.array.notifications_wpcom_settings_values)
                summaryArray = requireContext().resources.getStringArray(R.array.notifications_wpcom_settings_summaries)
            }
        }
        val shouldShowLocalNotifications =
            this.channel == NotificationsSettings.Channel.BLOGS && this.type == NotificationsSettings.Type.DEVICE
        if (settingsJson != null && mSettingsArray.size == mSettingsValues!!.size) {
            for (i in mSettingsArray.indices) {
                val settingName = mSettingsArray[i]!!
                val settingValue = mSettingsValues!![i]!!

                // Skip a few settings for 'Email' section
                if (this.type == NotificationsSettings.Type.EMAIL && settingValue == SETTING_VALUE_ACHIEVEMENT) {
                    continue
                }

                // Add special summary text for the WPCOM section
                var settingSummary: String? = null
                if (this.channel == NotificationsSettings.Channel.WPCOM && i < summaryArray.size) {
                    settingSummary = summaryArray[i]
                }
                val isSettingChecked = JSONUtils.queryJSON(settingsJson, settingValue, true)
                val isSettingLast = !shouldShowLocalNotifications && i == mSettingsArray.size - 1
                view.addView(
                    setupSwitchSettingView(
                        settingName, settingValue, settingSummary, isSettingChecked,
                        isSettingLast, mOnCheckedChangedListener
                    )
                )
            }
        }
        if (shouldShowLocalNotifications) {
            val isBloggingRemindersEnabled = bloggingRemindersProvider != null
            addWeeklyRoundupSetting(view, !isBloggingRemindersEnabled)
            if (isBloggingRemindersEnabled) {
                addBloggingReminderSetting(view)
            }
        }
        return view
    }

    private fun addWeeklyRoundupSetting(view: LinearLayout, isLast: Boolean) {
        view.addView(setupSwitchSettingView(
            requireContext().getString(R.string.weekly_roundup),
            null,
            null,
            AppPrefs.shouldShowWeeklyRoundupNotification(this.blogId),
            isLast
        ) { compoundButton: CompoundButton?, isChecked: Boolean ->
            AppPrefs.setShouldShowWeeklyRoundupNotification(
                this.blogId,
                isChecked
            )
        })
    }

    private fun addBloggingReminderSetting(view: LinearLayout) {
        view.addView(setupClickSettingView(
            requireContext().getString(R.string.site_settings_blogging_reminders_notification_title),
            this.bloggingRemindersProvider?.getSummary(this.blogId),
            true
        ) { v: View? ->
            this.bloggingRemindersProvider?.onClick(this.blogId)
            requireDialog().dismiss()
        })
    }

    private fun setupSwitchSettingView(settingName: String, settingValue: String?, settingSummary: String?,
                                       isSettingChecked: Boolean, isSettingLast: Boolean,
                                       onCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    ): View {
        return setupSettingView(
            settingName, settingValue, settingSummary, isSettingChecked,
            isSettingLast, onCheckedChangeListener, null
        )
    }

    private fun setupClickSettingView(settingName: String, settingSummary: String?, isSettingLast: Boolean,
                                      onClickListener: View.OnClickListener
    ): View {
        return setupSettingView(
            settingName, null, settingSummary, false,
            isSettingLast, null, onClickListener
        )
    }

    private fun setupSettingView(settingName: String, settingValue: String?, settingSummary: String?,
                                 isSettingChecked: Boolean, isSettingLast: Boolean,
                                 onCheckedChangeListener: CompoundButton.OnCheckedChangeListener?,
                                 onClickListener: View.OnClickListener?
    ): View {
        NotificationsSettingsSwitchBinding.inflate(layoutInflater).apply {
            notificationsSwitchTitle.text = settingName
            if (!TextUtils.isEmpty(settingSummary)) {
                notificationsSwitchSummary.visibility = View.VISIBLE
                notificationsSwitchSummary.text = settingSummary
            }
            if (onCheckedChangeListener != null) {
                notificationsSwitch.isChecked = isSettingChecked
                notificationsSwitch.tag = settingValue
                notificationsSwitch.setOnCheckedChangeListener(onCheckedChangeListener)
                rowContainer.setOnClickListener { v -> notificationsSwitch.toggle() }
            } else {
                notificationsSwitch.visibility = View.GONE
            }
            if (onClickListener != null) {
                rowContainer.setOnClickListener(onClickListener)
            }
            if (mShouldDisplayMainSwitch && isSettingLast) {
                val divider: View = notificationsListDivider
                val mlp = divider.layoutParams as ViewGroup.MarginLayoutParams
                mlp.leftMargin = 0
                mlp.rightMargin = 0
                divider.layoutParams = mlp
            }
            return root
        }
    }

    private val mOnCheckedChangedListener =
        CompoundButton.OnCheckedChangeListener { compoundButton, isChecked ->
            try {
                mUpdatedJson.put(compoundButton.tag.toString(), isChecked)

                // Switch off main switch if all current settings switches are off
                if (mMainSwitchToolbarView != null && !isChecked
                    && areAllSettingsSwitchesUnchecked()
                ) {
                    mMainSwitchToolbarView!!.setChecked(false)
                }
            } catch (e: JSONException) {
                AppLog.e(AppLog.T.NOTIFS, "Could not add notification setting change to JSONObject")
            }
        }

    override fun onMainSwitchCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        setSettingsSwitchesChecked(isChecked)
        hideDisabledView(isChecked)
    }

    /**
     * Hide view when Notifications Tab Settings are disabled by toggling the main switch off.
     *
     * @param isMainChecked TRUE to hide disabled view, FALSE to show disabled view
     */
    private fun hideDisabledView(isMainChecked: Boolean) {
        mDisabledView!!.visibility = if (isMainChecked) View.GONE else View.VISIBLE
        mOptionsView!!.visibility = if (isMainChecked) View.VISIBLE else View.GONE
    }

    /**
     * Updates Notifications current settings switches state based on the main switch state
     *
     * @param isMainChecked TRUE to switch on the settings switches.
     * FALSE to switch off the settings switches.
     */
    private fun setSettingsSwitchesChecked(isMainChecked: Boolean) {
        for (settingValue in mSettingsValues!!) {
            val toggleSwitch = mOptionsView!!.findViewWithTag<SwitchCompat>(settingValue)
            if (toggleSwitch != null) {
                toggleSwitch.isChecked = isMainChecked
            }
        }
    }

    // returns true if all current settings switches on the dialog are unchecked
    private fun areAllSettingsSwitchesUnchecked(): Boolean {
        var settingsSwitchesUnchecked = true
        for (settingValue in mSettingsValues!!) {
            val toggleSwitch = mOptionsView!!.findViewWithTag<SwitchCompat>(settingValue)
            if (toggleSwitch != null) {
                val isChecked = toggleSwitch.isChecked
                if (isChecked) {
                    settingsSwitchesUnchecked = false
                    break
                }
            }
        }
        return settingsSwitchesUnchecked
    }
}

