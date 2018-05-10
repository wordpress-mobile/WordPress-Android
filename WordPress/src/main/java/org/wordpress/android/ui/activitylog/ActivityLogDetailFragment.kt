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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_ID_KEY
import org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModel
import org.wordpress.android.widgets.WPNetworkImageView
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
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<ActivityLogDetailViewModel>(ActivityLogDetailViewModel::class.java)

        val intent = activity?.intent
        val (site, activityLogId) = when {
            savedInstanceState != null -> {
                val site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
                val activityLogId = savedInstanceState.getString(ACTIVITY_LOG_ID_KEY)
                site to activityLogId
            }
            intent != null -> {
                val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
                val activityLogId = intent.getStringExtra(ACTIVITY_LOG_ID_KEY)
                site to activityLogId
            }
            else -> throw Throwable("Couldn't initialize Activity Log view model")
        }
        viewModel.activityLogItem.observe(this, Observer { activityLogModel ->
            setActorIcon(activityLogModel?.actorIconUrl, activityLogModel?.showJetpackIcon)
            activity_actor_name.setTextOrHide(activityLogModel?.actorName)
            activity_actor_role.setTextOrHide(activityLogModel?.actorRole)

            activity_message.setTextOrHide(activityLogModel?.text)
            activity_type.setTextOrHide(activityLogModel?.summary)

            activity_created_date.text = activityLogModel?.createdDate
            activity_created_time.text = activityLogModel?.createdTime

            activity_rewind_button.setClickListenerOrHide(activityLogModel?.rewindAction)
        })
        viewModel.start(site, activityLogId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_item_detail, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, viewModel.site)
        outState.putString(ACTIVITY_LOG_ID_KEY, viewModel.activityLogId)
    }

    private fun setActorIcon(actorIcon: String?, showJetpackIcon: Boolean?) {
        when {
            actorIcon != null && actorIcon != "" -> {
                activity_actor_icon.setImageUrl(actorIcon, WPNetworkImageView.ImageType.AVATAR)
                activity_actor_icon.visibility = View.VISIBLE
                activity_jetpack_actor.visibility = View.GONE
            }
            showJetpackIcon == true -> {
                activity_jetpack_actor.visibility = View.VISIBLE
                activity_actor_icon.visibility = View.GONE
            }
            else -> {
                activity_actor_icon.resetImage()
                activity_actor_icon.visibility = View.GONE
                activity_jetpack_actor.visibility = View.GONE
            }
        }
    }

    private fun TextView.setTextOrHide(text: String?) {
        if (text != null) {
            this.text = text
            this.visibility = View.VISIBLE
        } else {
            this.visibility = View.GONE
        }
    }

    private fun View.setClickListenerOrHide(function: (() -> Unit)?) {
        if (function != null) {
            this.setOnClickListener {
                function()
            }
            this.visibility = View.VISIBLE
        } else {
            this.setOnClickListener(null)
            this.visibility = View.GONE
        }
    }
}
