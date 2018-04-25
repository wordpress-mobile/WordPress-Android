package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_log_item_detail.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModel
import org.wordpress.android.widgets.WPNetworkImageView
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ActivityLogDetailFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ActivityLogDetailViewModel

    companion object {
        fun newInstance(): ActivityLogDetailFragment {
            return ActivityLogDetailFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Use the same view model as the ActivityLogActivity
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<ActivityLogDetailViewModel>(ActivityLogDetailViewModel::class.java)

        val intent = activity?.intent
        when {
            savedInstanceState != null -> viewModel.readFromBundle(savedInstanceState)
            intent != null -> viewModel.readFromIntent(intent)
            else -> throw IllegalArgumentException("Couldn't initialize Activity Log view model")
        }
        viewModel.activityLogItem.observe(this, Observer { activityLogModel ->
            val actor = activityLogModel?.actor
            setActorIcon(actor?.avatarURL, activityLogModel?.gridicon)
            activity_actor_name.setTextOrHide(actor?.displayName)
            activity_actor_role.setTextOrHide(actor?.role)

            activity_message.setTextOrHide(activityLogModel?.text)
            activity_type.setTextOrHide(activityLogModel?.summary)

            activity_created_date.text = activityLogModel?.published?.printDate() ?: ""
            activity_created_time.text = activityLogModel?.published?.printTime() ?: ""

            activity_rewind_button.visibility = View.GONE
        })
        viewModel.start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_item_detail, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    private fun setActorIcon(actorIcon: String?, gridicon: String?) {
        if (actorIcon != null && gridicon == null) {
            activity_actor_icon.setImageUrl(actorIcon, WPNetworkImageView.ImageType.AVATAR)
        } else {
            activity_actor_icon.resetImage()
            activity_actor_icon.visibility = View.GONE
        }
    }

    private fun Date.printDate(): String {
        return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(this)
    }

    private fun Date.printTime(): String {
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(this)
    }

    private fun TextView.setTextOrHide(text: String?) {
        if (text != null) {
            this.text = text
        } else {
            this.visibility = View.GONE
        }
    }
}
