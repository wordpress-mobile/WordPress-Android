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
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingPromptsOnboardingDialogContentViewBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.avatars.AVATAR_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.avatars.AvatarItemDecorator
import org.wordpress.android.ui.avatars.TrainOfAvatarsAdapter
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.DismissDialog
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.featureintroduction.FeatureIntroductionDialogFragment
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class BloggingPromptsOnboardingDialogFragment : FeatureIntroductionDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: BloggingPromptsOnboardingViewModel
    private lateinit var dialogType: DialogType
    private val sitePickerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedSiteLocalId = result.data?.getIntExtra(
                    SitePickerActivity.KEY_SITE_LOCAL_ID,
                    SelectedSiteRepository.UNAVAILABLE
            ) ?: SelectedSiteRepository.UNAVAILABLE
            viewModel.onSiteSelected(selectedSiteLocalId)
        }
    }

    enum class DialogType {
        ONBOARDING,
        INFORMATION
    }

    companion object {
        const val TAG = "BLOGGING_PROMPTS_ONBOARDING_DIALOG_FRAGMENT"
        const val KEY_DIALOG_TYPE = "KEY_DIALOG_TYPE"

        @JvmStatic
        fun newInstance(type: DialogType): BloggingPromptsOnboardingDialogFragment =
                BloggingPromptsOnboardingDialogFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(KEY_DIALOG_TYPE, type)
                    }
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(BloggingPromptsOnboardingViewModel::class.java)
        setupHeaderTitle()
        setupHeaderIcon()
        setupUiStateObserver()
        setupActionObserver()
        viewModel.start(dialogType)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.let {
            dialogType = it.getSerializable(KEY_DIALOG_TYPE) as DialogType
        }
        (requireActivity().applicationContext as WordPress).component().inject(this)
        if (dialogType == ONBOARDING && context !is BloggingPromptsReminderSchedulerListener) {
            throw IllegalStateException(
                    "$context must implement ${BloggingPromptsReminderSchedulerListener::class.simpleName}"
            )
        }
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

            val layoutManager = FlexboxLayoutManager(
                    context,
                    FlexDirection.ROW,
                    FlexWrap.NOWRAP
            ).apply { justifyContent = JustifyContent.CENTER }
            promptCard.answeredUsersRecycler.addItemDecoration(
                    AvatarItemDecorator(RtlUtils.isRtl(context), requireContext(), AVATAR_LEFT_OFFSET_DIMEN)
            )
            promptCard.answeredUsersRecycler.layoutManager = layoutManager

            val adapter = TrainOfAvatarsAdapter(
                    imageManager,
                    uiHelpers
            )
            promptCard.answeredUsersRecycler.adapter = adapter
            adapter.loadData(readyState.respondents)

            setPrimaryButtonText(readyState.primaryButtonLabel)
            togglePrimaryButtonVisibility(readyState.isPrimaryButtonVisible)
            setPrimaryButtonListener { readyState.onPrimaryButtonClick() }

            setSecondaryButtonText(readyState.secondaryButtonLabel)
            toggleSecondaryButtonVisibility(readyState.isSecondaryButtonVisible)
            setSecondaryButtonListener { readyState.onSecondaryButtonClick() }

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
                is OpenEditor -> {
                    activity?.let {
                        startActivity(ActivityLauncher.openEditorWithContentIntent(it, action.content))
                    }
                }
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
                                .onSetPromptReminderClick(action.selectedSiteLocalId)
                    }
                }
                is DismissDialog -> {
                    dismiss()
                }
            }.exhaustive
        }
    }
}
