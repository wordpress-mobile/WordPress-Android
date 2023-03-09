package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.OFF
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.ONE_HOUR
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.TEN_MINUTES
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.WHEN_PUBLISHED
import org.wordpress.android.ui.posts.prepublishing.PrepublishingPublishSettingsViewModel
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

class PostNotificationScheduleTimeDialogFragment : DialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PublishSettingsViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val publishSettingsFragmentType = arguments?.getParcelableCompat<PublishSettingsFragmentType>(
            ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE
        )

        viewModel = when (publishSettingsFragmentType) {
            PublishSettingsFragmentType.EDIT_POST -> ViewModelProvider(
                requireActivity(),
                viewModelFactory
            )[EditPostPublishSettingsViewModel::class.java]
            PublishSettingsFragmentType.PREPUBLISHING_NUDGES -> ViewModelProvider(
                requireActivity(),
                viewModelFactory
            )[PrepublishingPublishSettingsViewModel::class.java]
            null -> error("PublishSettingsViewModel not initialized")
        }

        val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
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
        private const val ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE = "publish_settings_fragment_type"

        fun newInstance(
            period: SchedulingReminderModel.Period?,
            publishSettingsFragmentType: PublishSettingsFragmentType
        ): PostNotificationScheduleTimeDialogFragment {
            val fragment = PostNotificationScheduleTimeDialogFragment()
            val args = Bundle()
            args.putParcelable(
                ARG_PUBLISH_SETTINGS_FRAGMENT_TYPE,
                publishSettingsFragmentType
            )
            period?.let {
                args.putString(ARG_NOTIFICATION_SCHEDULE_TIME, period.name)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
