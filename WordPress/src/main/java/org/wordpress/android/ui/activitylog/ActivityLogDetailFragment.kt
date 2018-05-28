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
            activityActorName.setTextOrHide(activityLogModel?.actorName)
            activityActorRole.setTextOrHide(activityLogModel?.actorRole)

            activityMessage.setTextOrHide(activityLogModel?.text)
            activityType.setTextOrHide(activityLogModel?.summary)

            activityCreatedDate.text = activityLogModel?.createdDate
            activityCreatedTime.text = activityLogModel?.createdTime

            val rewindAndFinish = activityLogModel?.rewindAction?.let {
                rewindAction -> {
                    if (rewindAction()) activity?.finish()
                }
            }
            activityRewindButton.setClickListenerOrHide(rewindAndFinish)
        })
        viewModel.rewindAvailable.observe(this, Observer { available ->
            activityRewindButton.visibility = if (available == true) View.VISIBLE else View.GONE
        })
        viewModel.start(site, activityLogId)
    }

    override fun onDestroy() {
        viewModel.stop()
        super.onDestroy()
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
                activityActorIcon.setImageUrl(actorIcon, WPNetworkImageView.ImageType.AVATAR)
                activityActorIcon.visibility = View.VISIBLE
                activityJetpackActorIcon.visibility = View.GONE
            }
            showJetpackIcon == true -> {
                activityJetpackActorIcon.visibility = View.VISIBLE
                activityActorIcon.visibility = View.GONE
            }
            else -> {
                activityActorIcon.resetImage()
                activityActorIcon.visibility = View.GONE
                activityJetpackActorIcon.visibility = View.GONE
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
        } else {
            this.setOnClickListener(null)
        }
    }
}
