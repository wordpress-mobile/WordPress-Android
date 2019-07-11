package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import javax.inject.Inject

class EditPostPublishSettingsFragment : Fragment() {
    private lateinit var dateAndTime: TextView
    private lateinit var publishNotification: TextView
    private lateinit var publishNotificationContainer: LinearLayout
    private lateinit var addToCalendarContainer: LinearLayout
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(EditPostPublishSettingsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.edit_post_published_settings_fragment, container, false) as ViewGroup
        dateAndTime = rootView.findViewById(R.id.publish_time_and_date)
        val dateAndTimeContainer = rootView.findViewById<LinearLayout>(R.id.publish_time_and_date_container)
        publishNotification = rootView.findViewById(R.id.publish_notification)
        publishNotificationContainer = rootView.findViewById(R.id.publish_notification_container)
        addToCalendarContainer = rootView.findViewById(R.id.post_add_to_calendar_container)

        dateAndTimeContainer.setOnClickListener { showPostDateSelectionDialog() }

        viewModel.onDatePicked.observe(this, Observer {
            it?.applyIfNotHandled {
                showPostTimeSelectionDialog()
            }
        })
        viewModel.onPublishedDateChanged.observe(this, Observer {
            it?.let { date ->
                viewModel.updatePost(date, getPost(), activity!!)
            }
        })
        viewModel.onPublishedLabelChanged.observe(this, Observer {
            it?.let { label ->
                dateAndTime.text = label
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
        viewModel.start(getPost())
        return rootView
    }

    private fun showPostDateSelectionDialog() {
        if (!isAdded) {
            return
        }

        val fragment = PostDatePickerDialogFragment.newInstance()
        fragment.show(activity!!.supportFragmentManager, PostDatePickerDialogFragment.TAG)
    }

    private fun showPostTimeSelectionDialog() {
        if (!isAdded) {
            return
        }

        val fragment = PostTimePickerDialogFragment.newInstance()
        fragment.show(activity!!.supportFragmentManager, PostTimePickerDialogFragment.TAG)
    }

    private fun getPost(): PostModel? {
        return getEditPostActivityHook()?.post
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null

        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    companion object {
        fun newInstance() = EditPostPublishSettingsFragment()
    }
}
