package org.wordpress.android.ui.sitecreation.progress

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.FullscreenErrorWithRetryBinding
import org.wordpress.android.databinding.SiteCreationProgressCreatingSiteBinding
import org.wordpress.android.databinding.SiteCreationProgressScreenBinding
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationActivity.Companion.ARG_STATE
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Loading
import org.wordpress.android.ui.sitecreation.services.SiteCreationService
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AutoForeground.ServiceEventConnection
import org.wordpress.android.util.extensions.getParcelableCompat
import javax.inject.Inject

@AndroidEntryPoint
class SiteCreationProgressFragment : Fragment(R.layout.site_creation_progress_screen) {
    @Inject
    internal lateinit var uiHelpers: UiHelpers

    /**
     * We need to connect to the service, so the service knows when the app is in the background. The service
     * automatically shows system notifications when site creation is in progress and the app is in the background.
     */
    private var serviceEventConnection: ServiceEventConnection? = null
    private var animatorSet: AnimatorSet? = null

    private lateinit var binding: SiteCreationProgressScreenBinding
    private val viewModel: SiteCreationProgressViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is OnHelpClickedListener) { "Parent activity must implement OnHelpClickedListener." }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // we need to manually clear the service state to avoid sticky events from the previous SiteCreation flow.
            SiteCreationService.clearSiteCreationServiceState()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = SiteCreationProgressScreenBinding.bind(view).apply {
            observeState()
            observeHelpClicks(requireActivity() as OnHelpClickedListener)
            observeSiteCreationService()
            fullscreenErrorWithRetry.setOnClickListeners()
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        viewModel.start(requireNotNull(requireArguments().getParcelableCompat(ARG_STATE)))
    }

    private fun SiteCreationProgressScreenBinding.observeState() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.run {
                when (val ui = this@run) {
                    is Loading -> siteCreationProgressCreatingSite.updateLoadingLayout(ui)
                    is Error -> fullscreenErrorWithRetry.updateErrorLayout(ui)
                }
                siteCreationProgressCreatingSite.progressLayout.isVisible = progressLayoutVisibility
                fullscreenErrorWithRetry.errorLayout.isVisible = errorLayoutVisibility
            }
        }
    }

    private fun observeSiteCreationService() {
        viewModel.startCreateSiteService.observe(viewLifecycleOwner) { startServiceData ->
            startServiceData?.let {
                SiteCreationService.createSite(requireNotNull(activity), it.previousState, it.serviceData)
            }
        }
        viewModel.onFreeSiteCreated.observe(viewLifecycleOwner) {
            view?.announceForAccessibility(getString(R.string.new_site_creation_preview_title))
        }
    }

    private fun observeHelpClicks(listener: OnHelpClickedListener) {
        viewModel.onHelpClicked.observe(viewLifecycleOwner) {
            listener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_CREATING)
        }
    }

    private fun FullscreenErrorWithRetryBinding.setOnClickListeners() {
        errorRetry.setOnClickListener { viewModel.retry() }
        cancelWizardButton.setOnClickListener { viewModel.onCancelWizardClicked() }
        contactSupport.setOnClickListener { viewModel.onHelpClicked() }
    }

    private fun FullscreenErrorWithRetryBinding.updateErrorLayout(errorUiState: Error) {
        errorUiState.run {
            uiHelpers.setTextOrHide(errorTitle, titleResId)
            uiHelpers.setTextOrHide(errorSubtitle, subtitleResId)
            uiHelpers.updateVisibility(contactSupport, errorUiState.showContactSupport)
            uiHelpers.updateVisibility(cancelWizardButton, errorUiState.showCancelWizardButton)
        }
    }

    private fun SiteCreationProgressCreatingSiteBinding.updateLoadingLayout(
        progressUiState: Loading
    ) {
        progressUiState.apply {
            val newText = uiHelpers.getTextOfUiString(progressText.context, loadingTextResId)
            AppLog.d(AppLog.T.MAIN, "Changing text - animation: $animate")
            if (animate) {
                updateLoadingTextWithFadeAnimation(newText)
            } else {
                progressText.text = newText
            }
        }
    }

    private fun SiteCreationProgressCreatingSiteBinding.updateLoadingTextWithFadeAnimation(newText: CharSequence) {
        val animationDuration = AniUtils.Duration.SHORT
        val fadeOut = AniUtils.getFadeOutAnim(
            progressTextLayout,
            animationDuration,
            View.VISIBLE
        )
        val fadeIn = AniUtils.getFadeInAnim(
            progressTextLayout,
            animationDuration
        )

        // update the text when the view isn't visible
        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                progressText.text = newText
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                animatorSet = null
            }
        })
        // Start the fade-in animation right after the view fades out
        fadeIn.startDelay = animationDuration.toMillis(progressTextLayout.context)

        animatorSet = AnimatorSet().apply {
            playSequentially(fadeOut, fadeIn)
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEventConnection = ServiceEventConnection(context, SiteCreationService::class.java, viewModel)
    }

    override fun onPause() {
        super.onPause()
        serviceEventConnection?.disconnect(context, viewModel)
    }

    override fun onStop() {
        super.onStop()
        if (animatorSet?.isRunning == true) {
            animatorSet?.cancel()
        }
    }

    companion object {
        const val TAG = "site_creation_progress_fragment_tag"

        fun newInstance(siteCreationState: SiteCreationState) = SiteCreationProgressFragment()
            .apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_STATE, siteCreationState)
                }
            }
    }
}
