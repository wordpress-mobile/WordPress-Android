package org.wordpress.android.ui.bloggingprompts.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingPromptsOnboardingDialogContentViewBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.featureintroduction.FeatureIntroductionDialogFragment
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.extensions.exhaustive
import java.lang.IllegalStateException
import javax.inject.Inject

class BloggingPromptsOnboardingDialogFragment : FeatureIntroductionDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingPromptsOnboardingViewModel
    private val sitePickerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedSiteLocalId = result.data?.getIntExtra(
                    SitePickerActivity.KEY_SITE_LOCAL_ID,
                    SelectedSiteRepository.UNAVAILABLE
            ) ?: SelectedSiteRepository.UNAVAILABLE
            viewModel.onSiteSelected(selectedSiteLocalId)
        }
    }

    companion object {
        const val TAG = "BLOGGING_PROMPTS_ONBOARDING_DIALOG_FRAGMENT"

        @JvmStatic
        fun newInstance(): BloggingPromptsOnboardingDialogFragment = BloggingPromptsOnboardingDialogFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(BloggingPromptsOnboardingViewModel::class.java)
        setupTryNowButton()
        setupRemindMeButton()
        setupHeaderTitle()
        setupHeaderIcon()
        setupUiStateObserver()
        setupActionObserver()
        viewModel.start()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        if (context !is BloggingPromptsReminderSchedulerListener) {
            throw IllegalStateException(
                    "$context must implement ${BloggingPromptsReminderSchedulerListener::class.simpleName}"
            )
        }
    }

    private fun setupTryNowButton() {
        setPrimaryButtonListener { viewModel.onTryNowClick() }
        setPrimaryButtonText(R.string.blogging_prompts_onboarding_try_it_now)
    }

    private fun setupRemindMeButton() {
        setSecondaryButtonListener { viewModel.onRemindMeClick() }
        setSecondaryButtonText(R.string.blogging_prompts_onboarding_remind_me)
    }

    private fun setupHeaderTitle() {
        setHeaderTitle(R.string.blogging_prompts_onboarding_header_title)
    }

    private fun setupHeaderIcon() {
        setHeaderIcon(R.drawable.ic_outline_lightbulb_orange_gradient_40dp)
    }

    private fun setupContent(readyState: Ready) {
        val contentBinding = BloggingPromptsOnboardingDialogContentViewBinding.inflate(layoutInflater)
        setContent(contentBinding.root)
        with(contentBinding) {
            contentTop.text = getString(readyState.contentTopRes)
            cardCoverView.setOnClickListener { /*do nothing*/ }
            promptCard.promptContent.text = getString(readyState.promptRes)
            promptCard.numberOfAnswers.text = getString(readyState.answersRes, readyState.answersCount)
            contentBottom.text = getString(readyState.contentBottomRes)
            contentNote.text = buildSpannedString {
                bold { append("${getString(readyState.contentNoteTitle)} ") }
                append(getString(readyState.contentNoteContent))
            }
        }
    }

    private fun setupUiStateObserver() {
        viewModel.uiState.observe(this) { uiState ->
            when (uiState) {
                is Ready -> {
                    setupContent(uiState)
                }
            }.exhaustive
        }
    }

    private fun setupActionObserver() {
        viewModel.action.observe(this) { action ->
            when (action) {
                is OpenEditor -> ActivityLauncher.openEditorInNewStack(activity)
                is OpenSitePicker -> {
                    val intent = Intent(context, SitePickerActivity::class.java).apply {
                        putExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, action.selectedSite)
                        putExtra(SitePickerActivity.KEY_SITE_PICKER_MODE, SitePickerMode.DEFAULT_MODE)
                    }
                    sitePickerLauncher.launch(intent)
                }
                is OpenRemindersIntro -> {
                    activity?.let {
                        dismiss()
                        (it as BloggingPromptsReminderSchedulerListener)
                                .onSetPromptReminderClick(action.selectedSiteLocalId, action.isFirstTimePublishing)
                    }
                }
            }.exhaustive
        }
    }
}
