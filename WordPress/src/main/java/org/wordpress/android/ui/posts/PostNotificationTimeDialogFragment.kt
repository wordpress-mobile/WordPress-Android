package org.wordpress.android.ui.posts

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PostNotificationTimeDialogFragment.NotificationTime.OFF
import org.wordpress.android.ui.posts.PostNotificationTimeDialogFragment.NotificationTime.ONE_HOUR_BEFORE
import org.wordpress.android.ui.posts.PostNotificationTimeDialogFragment.NotificationTime.TEN_MINUTES_BEFORE
import org.wordpress.android.ui.posts.PostNotificationTimeDialogFragment.NotificationTime.WHEN_PUBLISHED
import javax.inject.Inject

class PostNotificationTimeDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(EditPostPublishSettingsViewModel::class.java)
        val alertDialogBuilder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.post_notification_type_selector, null) as RadioGroup
        alertDialogBuilder.setView(view)
        viewModel.onNotificationTime.observe(this, Observer { updatedNotificationTime ->
            val currentDataType = view.checkedRadioButtonId.toNotificationTime()
            if (updatedNotificationTime != currentDataType) {
                updatedNotificationTime?.let { view.check(updatedNotificationTime.toViewId()) }
            }
        })
        alertDialogBuilder.setTitle(R.string.stats_widget_select_type)

        alertDialogBuilder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
            viewModel.createNotification(view.checkedRadioButtonId.toNotificationTime())
            dialog?.dismiss()
        }
        return alertDialogBuilder.create()
    }

    private fun Int.toNotificationTime(): NotificationTime {
        return when (this) {
            R.id.off -> OFF
            R.id.one_hour_before -> ONE_HOUR_BEFORE
            R.id.ten_minutes_before -> TEN_MINUTES_BEFORE
            R.id.when_published -> WHEN_PUBLISHED
            else -> OFF
        }
    }

    enum class NotificationTime {
        OFF, ONE_HOUR_BEFORE, TEN_MINUTES_BEFORE, WHEN_PUBLISHED;

        fun toViewId(): Int {
            return when (this) {
                OFF -> R.id.off
                ONE_HOUR_BEFORE -> R.id.one_hour_before
                TEN_MINUTES_BEFORE -> R.id.ten_minutes_before
                WHEN_PUBLISHED -> R.id.when_published
            }
        }

        fun toLabel(): Int {
            return when (this) {
                OFF -> R.string.post_notification_off
                ONE_HOUR_BEFORE -> R.string.post_notification_one_hour_before
                TEN_MINUTES_BEFORE -> R.string.post_notification_ten_minutes_before
                WHEN_PUBLISHED -> R.string.post_notification_when_published
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (activity!!.applicationContext as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "post_notification_time_dialog_fragment"

        fun newInstance(): PostNotificationTimeDialogFragment {
            return PostNotificationTimeDialogFragment()
        }
    }
}
