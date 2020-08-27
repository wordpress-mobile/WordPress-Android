package org.wordpress.android.ui.activitylog.detail

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_log_item_detail.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.FormattableContentClickHandler
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_ID_KEY
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_REWIND_ID_KEY
import org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModel
import javax.inject.Inject

class ActivityLogDetailFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper
    @Inject lateinit var formattableContentClickHandler: FormattableContentClickHandler
    @Inject lateinit var uiHelpers: UiHelpers

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
        activity?.let { activity ->
            viewModel = ViewModelProviders.of(activity, viewModelFactory)
                    .get<ActivityLogDetailViewModel>(ActivityLogDetailViewModel::class.java)

            val intent = activity.intent
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

            viewModel.activityLogItem.observe(viewLifecycleOwner, Observer { activityLogModel ->
                setActorIcon(activityLogModel?.actorIconUrl, activityLogModel?.showJetpackIcon)
                uiHelpers.setTextOrHide(activityActorName, activityLogModel?.actorName)
                uiHelpers.setTextOrHide(activityActorRole, activityLogModel?.actorRole)

                val spannable = activityLogModel?.content?.let {
                    notificationsUtilsWrapper.getSpannableContentForRanges(it, activityMessage, { range ->
                        viewModel.onRangeClicked(range)
                    }, false)
                }

                val noteBlockSpans = spannable?.getSpans(0, spannable.length, NoteBlockClickableSpan::class.java)

                noteBlockSpans?.forEach {
                    it.enableColors(activity)
                }

                uiHelpers.setTextOrHide(activityMessage, spannable)
                uiHelpers.setTextOrHide(activityType, activityLogModel?.summary)

                activityCreatedDate.text = activityLogModel?.createdDate
                activityCreatedTime.text = activityLogModel?.createdTime

                if (activityLogModel != null) {
                    activityRewindButton.setOnClickListener {
                        viewModel.onRewindClicked(activityLogModel)
                    }
                }
            })

            viewModel.rewindAvailable.observe(viewLifecycleOwner, Observer { available ->
                activityRewindButton.visibility = if (available == true) View.VISIBLE else View.GONE
            })

            viewModel.showRewindDialog.observe(viewLifecycleOwner, Observer<ActivityLogDetailModel> { detailModel ->
                detailModel?.let { onRewindButtonClicked(it) }
            })

            viewModel.handleFormattableRangeClick.observe(viewLifecycleOwner, Observer<FormattableRange> { range ->
                if (range != null) {
                    formattableContentClickHandler.onClick(activity, range)
                }
            })

            viewModel.start(site, activityLogId)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_item_detail, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, viewModel.site)
        outState.putString(ACTIVITY_LOG_ID_KEY, viewModel.activityLogId)
    }

    fun onRewindConfirmed(rewindId: String) {
        val intent = activity?.intent?.putExtra(ACTIVITY_LOG_REWIND_ID_KEY, rewindId)
        activity?.setResult(RESULT_OK, intent)
        activity?.finish()
    }

    private fun setActorIcon(actorIcon: String?, showJetpackIcon: Boolean?) {
        when {
            actorIcon != null && actorIcon != "" -> {
                imageManager.loadIntoCircle(activityActorIcon, AVATAR_WITH_BACKGROUND, actorIcon)
                activityActorIcon.visibility = View.VISIBLE
                activityJetpackActorIcon.visibility = View.GONE
            }
            showJetpackIcon == true -> {
                activityJetpackActorIcon.visibility = View.VISIBLE
                activityActorIcon.visibility = View.GONE
            }
            else -> {
                imageManager.cancelRequestAndClearImageView(activityActorIcon)
                activityActorIcon.visibility = View.GONE
                activityJetpackActorIcon.visibility = View.GONE
            }
        }
    }

    private fun onRewindButtonClicked(item: ActivityLogDetailModel) {
        val dialog = BasicFragmentDialog()
        item.rewindId?.let {
            dialog.initialize(
                    it,
                    getString(R.string.activity_log_rewind_site),
                    getString(R.string.activity_log_rewind_dialog_message, item.createdDate, item.createdTime),
                    getString(R.string.activity_log_rewind_site),
                    getString(R.string.cancel)
            )
            dialog.show(requireFragmentManager(), it)
        }
    }
}
