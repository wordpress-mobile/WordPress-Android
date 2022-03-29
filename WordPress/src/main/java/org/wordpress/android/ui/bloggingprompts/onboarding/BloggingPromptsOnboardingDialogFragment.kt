package org.wordpress.android.ui.bloggingprompts.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.bloggingprompts.onboarding.card.BloggingPromptsOnboardingCardView
import org.wordpress.android.ui.featureintroduction.FeatureIntroductionDialogFragment
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
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

    private fun setupContent(readyState: Ready) {
        setContent {
            Column(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()) {
                Text(
                    text = stringResource(R.string.blogging_prompts_onboarding_body_top),
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = colorResource(R.color.black)
                )
                AndroidView(
                    modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .background(Color.Blue),
                    factory = { context ->
                        val cardView = BloggingPromptsOnboardingCardView(context)
                        cardView.bind(
                            BloggingPromptCardWithData(
                                prompt = readyState.prompt,
                                answeredUsers = readyState.answeredUsers,
                                numberOfAnswers = readyState.numberOfAnswers,
                                isAnswered = readyState.isAnswered
                            )
                        )
                        cardView
                    }
                )
                Text(
                    text = getString(R.string.blogging_prompts_onboarding_body_bottom),
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = colorResource(R.color.black)
                )
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
                is OpenSitePicker -> { /*TODO*/
                }
                is OpenRemindersIntro -> { /*TODO*/
                }
            }.exhaustive
        }
    }
}
