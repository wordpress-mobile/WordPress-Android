package org.wordpress.android.ui.bloggingprompts.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.featureintroduction.FeatureIntroductionDialogFragment
import org.wordpress.android.util.extensions.exhaustive
import javax.inject.Inject

class BloggingPromptsOnboardingDialogFragment : FeatureIntroductionDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingPromptsOnboardingViewModel

    companion object {
        const val TAG = "BLOGGING_PROMPTS_ONBOARDING_DIALOG_FRAGMENT"

        @JvmStatic
        fun newInstance(): BloggingPromptsOnboardingDialogFragment = BloggingPromptsOnboardingDialogFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTryNowButton()
        setupRemindMeButton()
        setupHeaderTitle()
        setupHeaderIcon()
        setupContent()
        setupActionObserver()
        viewModel.start()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    private fun setupTryNowButton() {
        setPrimaryButtonListener { viewModel.onTryNowClick() }
        setPrimaryButtonText(R.string.blogging_prompts_onboarding_try_it_now)

    }

    private fun setupRemindMeButton() {
        setPrimaryButtonListener { viewModel.onRemindMeClick() }
        setPrimaryButtonText(R.string.blogging_prompts_onboarding_remind_me)
    }

    private fun setupHeaderTitle() {
        setHeaderTitle(R.string.blogging_prompts_onboarding_header_title)
    }

    private fun setupHeaderIcon() {
        setHeaderIcon(R.drawable.ic_story_icon_24dp)
    }

    private fun setupContent() {
        setContent {
            
        }
    }

    private fun setupActionObserver() {
        viewModel.action.observe(this, { action ->
            when (action) {
                is OpenEditor -> ActivityLauncher.openEditorInNewStack(activity)
                is OpenSitePicker -> { /*TODO*/
                }
                is OpenRemindersIntro -> { /*TODO*/
                }
            }.exhaustive
        })
    }
}
