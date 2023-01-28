package org.wordpress.android.ui.activitylog.detail

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ActivityLogItemDetailBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityLauncher.BACKUP_TRACK_EVENT_PROPERTY_VALUE
import org.wordpress.android.ui.ActivityLauncher.SOURCE_TRACK_EVENT_PROPERTY_KEY
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents.ShowBackupDownload
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents.ShowDocumentationPage
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents.ShowRestore
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.FormattableContentClickHandler
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_ID_KEY
import org.wordpress.android.viewmodel.activitylog.ACTIVITY_LOG_IS_RESTORE_HIDDEN_KEY
import org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModel
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

private const val DETAIL_TRACKING_SOURCE = "detail"
private const val FORWARD_SLASH = "/"

@AndroidEntryPoint
class ActivityLogDetailFragment : Fragment(R.layout.activity_log_item_detail) {
    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper

    @Inject
    lateinit var formattableContentClickHandler: FormattableContentClickHandler

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    private val viewModel: ActivityLogDetailViewModel by viewModels()

    private val trackingSource by lazy {
        requireActivity().intent?.extras?.getString(SOURCE_TRACK_EVENT_PROPERTY_KEY)
    }

    companion object {
        fun newInstance(): ActivityLogDetailFragment {
            return ActivityLogDetailFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(ActivityLogItemDetailBinding.bind(view)) {
            setupViews(savedInstanceState)
            setupObservers()
        }
    }

    private fun ActivityLogItemDetailBinding.setupViews(savedInstanceState: Bundle?) {
        activity?.let { activity ->
            val (site, activityLogId) = sideAndActivityId(savedInstanceState, activity.intent)
            val areButtonsVisible = areButtonsVisible(savedInstanceState, activity.intent)
            val isRestoreHidden = isRestoreHidden(savedInstanceState, activity.intent)

            site?.let { siteModel ->  viewModel.start(siteModel, activityLogId, areButtonsVisible, isRestoreHidden) }
        }

        if (jetpackBrandingUtils.shouldShowJetpackBranding()) {
            val screen = trackingSource
                ?.takeIf { it == BACKUP_TRACK_EVENT_PROPERTY_VALUE }
                ?.let { JetpackPoweredScreen.WithDynamicText.BACKUP_DETAIL }
                ?: JetpackPoweredScreen.WithDynamicText.ACTIVITY_LOG_DETAIL

            jetpackBadge.root.isVisible = true
            jetpackBadge.jetpackPoweredBadge.text = uiHelpers.getTextOfUiString(
                requireContext(),
                jetpackBrandingUtils.getBrandingTextForScreen(screen)
            )

            if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                jetpackBadge.jetpackPoweredBadge.setOnClickListener {
                    jetpackBrandingUtils.trackBadgeTapped(screen)
                    viewModel.showJetpackPoweredBottomSheet()
                }
            }
        }
    }

    private fun ActivityLogItemDetailBinding.setupObservers() {
        viewModel.activityLogItem.observe(viewLifecycleOwner, { activityLogModel ->
            loadLogItem(activityLogModel, requireActivity())
        })

        viewModel.restoreVisible.observe(viewLifecycleOwner, { available ->
            activityRestoreButton.visibility = if (available == true) View.VISIBLE else View.GONE
        })
        viewModel.downloadBackupVisible.observe(viewLifecycleOwner, { available ->
            activityDownloadBackupButton.visibility = if (available == true) View.VISIBLE else View.GONE
        })
        viewModel.multisiteVisible.observe(viewLifecycleOwner, { available ->
            checkAndShowMultisiteMessage(available)
        })

        viewModel.navigationEvents.observeEvent(viewLifecycleOwner, {
            when (it) {
                is ShowBackupDownload -> ActivityLauncher.showBackupDownloadForResult(
                    requireActivity(),
                    viewModel.site,
                    it.model.activityID,
                    RequestCodes.BACKUP_DOWNLOAD,
                    buildTrackingSource()
                )
                is ShowRestore -> ActivityLauncher.showRestoreForResult(
                    requireActivity(),
                    viewModel.site,
                    it.model.activityID,
                    RequestCodes.RESTORE,
                    buildTrackingSource()
                )
                is ShowDocumentationPage -> ActivityLauncher.openUrlExternal(requireContext(), it.url)
            }
        })

        viewModel.handleFormattableRangeClick.observe(viewLifecycleOwner, { range ->
            if (range != null) {
                formattableContentClickHandler.onClick(
                    requireActivity(),
                    range,
                    ReaderTracker.SOURCE_ACTIVITY_LOG_DETAIL
                )
            }
        })

        viewModel.showJetpackPoweredBottomSheet.observeEvent(viewLifecycleOwner) {
            JetpackPoweredBottomSheetFragment
                .newInstance()
                .show(childFragmentManager, JetpackPoweredBottomSheetFragment.TAG)
        }
    }

    private fun ActivityLogItemDetailBinding.checkAndShowMultisiteMessage(available: Pair<Boolean, SpannableString?>) {
        if (available.first) {
            with(multisiteMessage) {
                linksClickable = true
                isClickable = true
                movementMethod = LinkMovementMethod.getInstance()
                text = available.second
                visibility = View.VISIBLE
            }
        } else {
            multisiteMessage.visibility = View.GONE
        }
    }

    private fun ActivityLogItemDetailBinding.loadLogItem(
        activityLogModel: ActivityLogDetailModel?,
        activity: FragmentActivity
    ) {
        setActorIcon(activityLogModel?.actorIconUrl, activityLogModel?.showJetpackIcon)
        uiHelpers.setTextOrHide(activityActorName, activityLogModel?.actorName)
        uiHelpers.setTextOrHide(activityActorRole, activityLogModel?.actorRole)

        val spannable = activityLogModel?.content?.let {
            notificationsUtilsWrapper.getSpannableContentForRanges(
                it,
                activityMessage,
                { range ->
                    viewModel.onRangeClicked(range)
                },
                false
            )
        }

        val noteBlockSpans = spannable?.getSpans(
            0,
            spannable.length,
            NoteBlockClickableSpan::class.java
        )

        noteBlockSpans?.forEach {
            it.enableColors(activity)
        }

        uiHelpers.setTextOrHide(activityMessage, spannable)
        uiHelpers.setTextOrHide(activityType, activityLogModel?.summary)

        activityCreatedDate.text = activityLogModel?.createdDate
        activityCreatedTime.text = activityLogModel?.createdTime

        if (activityLogModel != null) {
            activityRestoreButton.setOnClickListener {
                viewModel.onRestoreClicked(activityLogModel)
            }
            activityDownloadBackupButton.setOnClickListener {
                viewModel.onDownloadBackupClicked(activityLogModel)
            }
        }
    }

    private fun sideAndActivityId(savedInstanceState: Bundle?, intent: Intent?) = when {
        savedInstanceState != null -> {
            val site = savedInstanceState.getSerializableCompat<SiteModel>(WordPress.SITE)
            val activityLogId = requireNotNull(
                savedInstanceState.getString(
                    ACTIVITY_LOG_ID_KEY
                )
            )
            site to activityLogId
        }
        intent != null -> {
            val site = intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE)
            val activityLogId = intent.getStringExtra(ACTIVITY_LOG_ID_KEY) as String
            site to activityLogId
        }
        else -> throw Throwable("Couldn't initialize Activity Log view model")
    }

    private fun areButtonsVisible(savedInstanceState: Bundle?, intent: Intent?) = when {
        savedInstanceState != null ->
            requireNotNull(savedInstanceState.getBoolean(ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY, true))
        intent != null ->
            intent.getBooleanExtra(ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY, true)
        else -> throw Throwable("Couldn't initialize Activity Log view model")
    }

    private fun isRestoreHidden(savedInstanceState: Bundle?, intent: Intent?) = when {
        savedInstanceState != null ->
            requireNotNull(savedInstanceState.getBoolean(ACTIVITY_LOG_IS_RESTORE_HIDDEN_KEY, false))
        intent != null ->
            intent.getBooleanExtra(ACTIVITY_LOG_IS_RESTORE_HIDDEN_KEY, false)
        else -> throw Throwable("Couldn't initialize Activity Log view model")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, viewModel.site)
        outState.putString(ACTIVITY_LOG_ID_KEY, viewModel.activityLogId)
        outState.putBoolean(ACTIVITY_LOG_ARE_BUTTONS_VISIBLE_KEY, viewModel.areButtonsVisible)
        outState.putBoolean(ACTIVITY_LOG_IS_RESTORE_HIDDEN_KEY, viewModel.isRestoreHidden)
    }

    private fun ActivityLogItemDetailBinding.setActorIcon(actorIcon: String?, showJetpackIcon: Boolean?) {
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

    private fun buildTrackingSource() = trackingSource.let { source ->
        when {
            source != null -> source + FORWARD_SLASH + DETAIL_TRACKING_SOURCE
            else -> DETAIL_TRACKING_SOURCE
        }
    }
}
