package org.wordpress.android.ui.prefs.notifications

import android.graphics.PorterDuff
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.models.NotificationsSettings
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.bloggingreminders.BloggingReminderUtils
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.extensions.getColorStateListFromAttribute
import javax.inject.Inject

class NotificationsSettingsTypesFragment: ChildNotificationSettingsFragment() {
    companion object {
        const val ARG_BLOG_ID = "ARG_BLOG_ID"
        const val ARG_NOTIFICATION_CHANNEL = "ARG_NOTIFICATION_CHANNEL"
        const val ARG_NOTIFICATIONS_ENABLED = "ARG_NOTIFICATIONS_ENABLED"

        private const val BLOGGING_REMINDERS_BOTTOM_SHEET_TAG = "blogging-reminders-dialog-tag"
    }

    @Inject
    lateinit var mViewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var mUiHelpers: UiHelpers

    private var mDeviceId: String? = null
    private var mNotificationsSettings: NotificationsSettings? = null
    private var mNotificationsEnabled: Boolean = false
    private var mBloggingRemindersViewModel: BloggingRemindersViewModel? = null
    private val mBloggingRemindersSummariesBySiteId: MutableMap<Long?, UiString?> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBloggingReminders()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings_types, rootKey)

        loadNotificationsSettings()

        val settings = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        mDeviceId = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_SERVER_ID, "")

        val blogId: Long
        val channel: NotificationsSettings.Channel
        requireArguments().apply {
            blogId = getLong(ARG_BLOG_ID)
            channel = NotificationsSettings.Channel.toNotificationChannel(getInt(ARG_NOTIFICATION_CHANNEL))
            mNotificationsEnabled = getBoolean(ARG_NOTIFICATIONS_ENABLED)
        }

        val context = requireContext()
        val category = PreferenceCategory(context)
        category.setTitle(R.string.notification_types)
        preferenceScreen.addPreference(category)

        val timelinePreference = NotificationsSettingsDialogPreferenceX(
            context = context, attrs = null, channel = channel, type = NotificationsSettings.Type.TIMELINE,
            blogId = blogId, settings = mNotificationsSettings!!, listener = mOnSettingsChangedListener,
            dialogTitleRes = R.string.notifications_tab
        ).apply {
            setPreferenceIcon(R.drawable.ic_bell_white_24dp)
            setTitle(R.string.notifications_tab)
            setSummary(R.string.notifications_tab_summary)
            key = getString(R.string.notifications_tab)
        }
        category.addPreference(timelinePreference)

        val emailPreference = NotificationsSettingsDialogPreferenceX(
            context = context, attrs = null, channel = channel, type = NotificationsSettings.Type.EMAIL,
            blogId = blogId, settings = mNotificationsSettings!!, listener = mOnSettingsChangedListener,
            dialogTitleRes = R.string.email
        ).apply {
            setPreferenceIcon(R.drawable.ic_mail_white_24dp)
            setTitle(R.string.email)
            setSummary(R.string.notifications_email_summary)
            key = getString(R.string.email)
        }
        category.addPreference(emailPreference)

        if (!TextUtils.isEmpty(mDeviceId)) {
            val devicePreference = NotificationsSettingsDialogPreferenceX(
                context = context, attrs = null, channel = channel, type = NotificationsSettings.Type.DEVICE,
                blogId = blogId, settings = mNotificationsSettings!!, listener = mOnSettingsChangedListener,
                bloggingRemindersProvider = mBloggingRemindersProvider, dialogTitleRes = R.string.app_notifications
            ).apply {
                setPreferenceIcon(R.drawable.ic_phone_white_24dp)
                setTitle(R.string.app_notifications)
                setSummary(R.string.notifications_push_summary)
                key = getString(R.string.app_notifications)
                isEnabled = mNotificationsEnabled
            }
            category.addPreference(devicePreference)
        }
    }

    @Suppress("DEPRECATION", "Warnings")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is NotificationsSettingsDialogPreferenceX) {
            if (parentFragmentManager.findFragmentByTag(NotificationsSettingsDialogFragment.TAG) != null) {
                return
            }

            with(preference) {
                NotificationsSettingsDialogFragment(
                    channel = channel,
                    type = type,
                    blogId = blogId,
                    settings = settings,
                    onNotificationsSettingsChangedListener = listener,
                    bloggingRemindersProvider = bloggingRemindersProvider,
                    title = context.getString(dialogTitleRes)
                ).apply {
                    setTargetFragment(
                        this@NotificationsSettingsTypesFragment,
                        RequestCodes.NOTIFICATION_SETTINGS
                    )
                }.show(
                    parentFragmentManager,
                    NotificationsSettingsDialogFragment.TAG
                )
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private val mOnSettingsChangedListener =
        NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener { channel, type, blogId,
                                                                                       newValues ->
            if (!isAdded) {
                return@OnNotificationsSettingsChangedListener
            }

            // Construct a new settings JSONObject to send back to WP.com
            val settingsObject = JSONObject()
            when (channel!!) {
                NotificationsSettings.Channel.BLOGS -> try {
                    val blogObject = JSONObject()
                    blogObject.put(NotificationsSettings.KEY_BLOG_ID, blogId)
                    val blogsArray = JSONArray()
                    if (type == NotificationsSettings.Type.DEVICE) {
                        newValues.put(NotificationsSettings.KEY_DEVICE_ID, mDeviceId!!.toLong())
                        val devicesArray = JSONArray()
                        devicesArray.put(newValues)
                        blogObject.put(NotificationsSettings.KEY_DEVICES, devicesArray)
                        blogsArray.put(blogObject)
                    } else {
                        blogObject.put(type.toString(), newValues)
                        blogsArray.put(blogObject)
                    }
                    settingsObject.put(NotificationsSettings.KEY_BLOGS, blogsArray)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not build notification settings object")
                }

                NotificationsSettings.Channel.OTHER -> try {
                    val otherObject = JSONObject()
                    if (type == NotificationsSettings.Type.DEVICE) {
                        newValues.put(NotificationsSettings.KEY_DEVICE_ID, mDeviceId!!.toLong())
                        val devicesArray = JSONArray()
                        devicesArray.put(newValues)
                        otherObject.put(NotificationsSettings.KEY_DEVICES, devicesArray)
                    } else {
                        otherObject.put(type.toString(), newValues)
                    }
                    settingsObject.put(NotificationsSettings.KEY_OTHER, otherObject)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not build notification settings object")
                }

                NotificationsSettings.Channel.WPCOM -> try {
                    settingsObject.put(NotificationsSettings.KEY_WPCOM, newValues)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not build notification settings object")
                }
            }
            if (settingsObject.length() > 0) {
                WordPress.getRestClientUtilsV1_1()
                    .post("/me/notifications/settings", settingsObject, null, null, null)
            }
        }

    private val mBloggingRemindersProvider: NotificationsSettingsDialogPreference.BloggingRemindersProvider = object :
        NotificationsSettingsDialogPreference.BloggingRemindersProvider {
        override fun getSummary(blogId: Long): String? {
            val uiString = mBloggingRemindersSummariesBySiteId[blogId]
            return if (uiString != null) mUiHelpers.getTextOfUiString(requireContext(), uiString).toString() else null
        }

        override fun onClick(blogId: Long) {
            mBloggingRemindersViewModel!!.onNotificationSettingsItemClicked(blogId)
        }
    }

    private fun initBloggingReminders() {
        if (!isAdded) {
            return
        }
        (activity as AppCompatActivity?)?.let { activity ->
            mBloggingRemindersViewModel = ViewModelProvider(
                activity,
                mViewModelFactory
            )[BloggingRemindersViewModel::class.java]
            BloggingReminderUtils.observeBottomSheet(
                mBloggingRemindersViewModel!!.isBottomSheetShowing,
                activity,
                BLOGGING_REMINDERS_BOTTOM_SHEET_TAG
            ) { activity.supportFragmentManager }
            mBloggingRemindersViewModel!!.notificationsSettingsUiState
                .observe(activity) { map ->
                    mBloggingRemindersSummariesBySiteId.putAll(map)
                }
        }
    }

    private fun loadNotificationsSettings() {
        val settingsJson: JSONObject = try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            JSONObject(
                sharedPreferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, "")!!
            )
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.NOTIFS, "Could not parse notifications settings JSON")
            return
        }
        if (mNotificationsSettings == null) {
            mNotificationsSettings = NotificationsSettings(settingsJson)
        } else {
            mNotificationsSettings!!.updateJson(settingsJson)
        }
    }

    private fun NotificationsSettingsDialogPreferenceX.setPreferenceIcon(@DrawableRes drawableRes: Int) {
        setIcon(drawableRes)
        icon?.setTintMode(PorterDuff.Mode.SRC_IN)
        icon?.setTintList(context.getColorStateListFromAttribute(R.attr.wpColorOnSurfaceMedium))
    }
}
