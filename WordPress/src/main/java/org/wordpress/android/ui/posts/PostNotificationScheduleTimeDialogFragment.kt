package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.OFF
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.ONE_HOUR
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.TEN_MINUTES
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.WHEN_PUBLISHED
import javax.inject.Inject

class PostNotificationScheduleTimeDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(EditPostPublishSettingsViewModel::class.java)
        val alertDialogBuilder = MaterialAlertDialogBuilder(activity)
        val view = requireActivity().layoutInflater.inflate(R.layout.post_notification_type_selector, null)
                as RadioGroup
        alertDialogBuilder.setView(view)
        val notificationTime = arguments?.getString(ARG_NOTIFICATION_SCHEDULE_TIME)?.let {
            SchedulingReminderModel.Period.valueOf(
                    it
            )
        } ?: OFF
        view.check(notificationTime.toViewId())
        alertDialogBuilder.setTitle(R.string.post_settings_notification)

        alertDialogBuilder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
            viewModel.onNotificationCreated(view.checkedRadioButtonId.toDialogModel())
            dialog?.dismiss()
        }
        return alertDialogBuilder.create()
    }

    private fun Int.toDialogModel(): SchedulingReminderModel.Period {
        return when (this) {
            R.id.off -> OFF
            R.id.one_hour_before -> ONE_HOUR
            R.id.ten_minutes_before -> TEN_MINUTES
            R.id.when_published -> WHEN_PUBLISHED
            else -> OFF
        }
    }

    fun SchedulingReminderModel.Period.toViewId(): Int {
        return when (this) {
            OFF -> R.id.off
            ONE_HOUR -> R.id.one_hour_before
            TEN_MINUTES -> R.id.ten_minutes_before
            WHEN_PUBLISHED -> R.id.when_published
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "post_notification_time_dialog_fragment"
        const val ARG_NOTIFICATION_SCHEDULE_TIME = "notification_schedule_time"

        fun newInstance(period: SchedulingReminderModel.Period?): PostNotificationScheduleTimeDialogFragment {
            val fragment = PostNotificationScheduleTimeDialogFragment()
            period?.let {
                val args = Bundle()
                args.putString(ARG_NOTIFICATION_SCHEDULE_TIME, period.name)
                fragment.arguments = args
            }
            return fragment
        }
    }
}
