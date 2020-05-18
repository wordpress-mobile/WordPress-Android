package org.wordpress.android.ui.posts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PublishSettingsFragmentType.EDIT_POST
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

abstract class PublishSettingsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    lateinit var viewModel: PublishSettingsViewModel

    @LayoutRes protected abstract fun getContentLayout(): Int

    protected abstract fun getPublishSettingsFragmentType(): PublishSettingsFragmentType

    protected abstract fun setupContent(
        rootView: ViewGroup,
        viewModel: PublishSettingsViewModel
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(getContentLayout(), container, false) as ViewGroup
        val dateAndTime = rootView.findViewById<TextView>(R.id.publish_time_and_date)
        val dateAndTimeContainer = rootView.findViewById<LinearLayout>(R.id.publish_time_and_date_container)
        val publishNotification = rootView.findViewById<TextView>(R.id.publish_notification)
        val publishNotificationTitle = rootView.findViewById<TextView>(R.id.publish_notification_title)
        val publishNotificationContainer = rootView.findViewById<LinearLayout>(R.id.publish_notification_container)
        val addToCalendarContainer = rootView.findViewById<LinearLayout>(R.id.post_add_to_calendar_container)
        val addToCalendar = rootView.findViewById<TextView>(R.id.post_add_to_calendar)

        AccessibilityUtils.disableHintAnnouncement(dateAndTime)
        AccessibilityUtils.disableHintAnnouncement(publishNotification)

        dateAndTimeContainer.setOnClickListener { showPostDateSelectionDialog() }

        setupContent(rootView, viewModel)

        viewModel.onDatePicked.observe(this, Observer {
            it?.applyIfNotHandled {
                showPostTimeSelectionDialog()
            }
        })
        viewModel.onPublishedDateChanged.observe(this, Observer {
            it?.let { date ->
                viewModel.updatePost(date, getPostRepository())
                trackPostScheduled()
            }
        })
        viewModel.onNotificationTime.observe(this, Observer {
            it?.let { notificationTime ->
                getPostRepository()?.let { postRepository ->
                    viewModel.scheduleNotification(postRepository, notificationTime)
                }
            }
        })
        viewModel.onUiModel.observe(this, Observer {
            it?.let { uiModel ->
                dateAndTime.text = uiModel.publishDateLabel
                publishNotificationTitle.isEnabled = uiModel.notificationEnabled
                publishNotification.isEnabled = uiModel.notificationEnabled
                publishNotificationContainer.isEnabled = uiModel.notificationEnabled
                addToCalendar.isEnabled = uiModel.notificationEnabled
                addToCalendarContainer.isEnabled = uiModel.notificationEnabled
                if (uiModel.notificationEnabled) {
                    publishNotificationContainer.setOnClickListener {
                        getPostRepository()?.getPost()?.let { postModel ->
                            viewModel.onShowDialog(postModel)
                        }
                    }
                    addToCalendarContainer.setOnClickListener {
                        getPostRepository()?.let { postRepository ->
                            viewModel.onAddToCalendar(postRepository)
                        }
                    }
                } else {
                    publishNotificationContainer.setOnClickListener(null)
                    addToCalendarContainer.setOnClickListener(null)
                }
                publishNotification.setText(uiModel.notificationLabel)
                publishNotificationContainer.visibility = if (uiModel.notificationVisible) View.VISIBLE else View.GONE
                addToCalendarContainer.visibility = if (uiModel.notificationVisible) View.VISIBLE else View.GONE
            }
        })
        viewModel.onShowNotificationDialog.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { notificationTime ->
                showNotificationTimeSelectionDialog(notificationTime)
            }
        })
        viewModel.onToast.observe(this, Observer {
            it?.applyIfNotHandled {
                ToastUtils.showToast(
                        context,
                        this,
                        SHORT,
                        Gravity.TOP
                )
            }
        })
        viewModel.onNotificationAdded.observe(this, Observer { event ->
            event?.getContentIfNotHandled()?.let { notification ->
                activity?.let {
                    NotificationManagerCompat.from(it).cancel(notification.id)
                    val notificationIntent = Intent(it, PublishNotificationReceiver::class.java)
                    notificationIntent.putExtra(PublishNotificationReceiver.NOTIFICATION_ID, notification.id)
                    val pendingIntent = PendingIntent.getBroadcast(
                            it,
                            notification.id,
                            notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    )

                    val alarmManager = it.getSystemService(ALARM_SERVICE) as AlarmManager
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            notification.scheduledTime,
                            pendingIntent
                    )
                }
            }
        })
        viewModel.onAddToCalendar.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { calendarEvent ->
                val calIntent = Intent(Intent.ACTION_INSERT)
                calIntent.data = Events.CONTENT_URI
                calIntent.type = "vnd.android.cursor.item/event"
                calIntent.putExtra(Events.TITLE, calendarEvent.title)
                calIntent.putExtra(Events.DESCRIPTION, calendarEvent.description)
                calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarEvent.startTime)
                startActivity(calIntent)
            }
        })
        viewModel.start(getPostRepository())
        return rootView
    }

    private fun trackPostScheduled() {
        when (getPublishSettingsFragmentType()) {
            EDIT_POST -> {
                analyticsTrackerWrapper.trackPostSettings(Stat.EDITOR_POST_SCHEDULE_CHANGED)
            }
            PublishSettingsFragmentType.PREPUBLISHING_NUDGES -> {
                analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_SCHEDULE_CHANGED)
            }
        }
    }

    private fun showPostDateSelectionDialog() {
        if (!isAdded) {
            return
        }

        val fragment = PostDatePickerDialogFragment.newInstance(getPublishSettingsFragmentType())
        fragment.show(activity!!.supportFragmentManager, PostDatePickerDialogFragment.TAG)
    }

    private fun showPostTimeSelectionDialog() {
        if (!isAdded) {
            return
        }

        val fragment = PostTimePickerDialogFragment.newInstance(getPublishSettingsFragmentType())
        fragment.show(activity!!.supportFragmentManager, PostTimePickerDialogFragment.TAG)
    }

    private fun showNotificationTimeSelectionDialog(schedulingReminderPeriod: SchedulingReminderModel.Period?) {
        if (!isAdded) {
            return
        }

        val fragment = PostNotificationScheduleTimeDialogFragment.newInstance(
                schedulingReminderPeriod,
                getPublishSettingsFragmentType()
        )
        fragment.show(activity!!.supportFragmentManager, PostNotificationScheduleTimeDialogFragment.TAG)
    }

    private fun getPostRepository(): EditPostRepository? {
        return getEditPostActivityHook()?.editPostRepository
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null

        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }
}

@Parcelize
enum class PublishSettingsFragmentType : Parcelable {
    EDIT_POST,
    PREPUBLISHING_NUDGES
}
