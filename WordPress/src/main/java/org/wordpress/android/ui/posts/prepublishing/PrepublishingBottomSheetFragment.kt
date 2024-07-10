package org.wordpress.android.ui.posts.prepublishing

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.PostPrepublishingBottomSheetBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPBottomSheetDialogFragment
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent.OpenEditShareMessage
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent.OpenSocialConnectionsList
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent.OpenSubscribeJetpackSocial
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.ADD_CATEGORY
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.CATEGORIES
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.PUBLISH
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.SOCIAL
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen.TAGS
import org.wordpress.android.ui.posts.prepublishing.categories.PrepublishingCategoriesFragment
import org.wordpress.android.ui.posts.prepublishing.categories.addcategory.PrepublishingAddCategoryFragment
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeFragment
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.prepublishing.home.PublishPost
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingActionClickedListener
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingBottomSheetListener
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingScreenClosedListener
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingSocialViewModelProvider
import org.wordpress.android.ui.posts.prepublishing.publishsettings.PrepublishingPublishSettingsFragment
import org.wordpress.android.ui.posts.prepublishing.social.PrepublishingSocialFragment
import org.wordpress.android.ui.posts.prepublishing.tags.PrepublishingTagsFragment
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageActivity
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageActivity.Companion.createIntent
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.KeyboardResizeViewUtil
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject
import com.google.android.material.R as MaterialR

class PrepublishingBottomSheetFragment : WPBottomSheetDialogFragment(),
    PrepublishingScreenClosedListener, PrepublishingActionClickedListener, PrepublishingSocialViewModelProvider {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var viewModel: PrepublishingViewModel
    private lateinit var keyboardResizeViewUtil: KeyboardResizeViewUtil

    private var prepublishingBottomSheetListener: PrepublishingBottomSheetListener? = null

    private var jetpackSocialViewModel: EditorJetpackSocialViewModel? = null
    private lateinit var editShareMessageActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prepublishingBottomSheetListener = if (context is PrepublishingBottomSheetListener) {
            context
        } else {
            throw RuntimeException("$context must implement PrepublishingBottomSheetListener")
        }

        editShareMessageActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    val shareMessage = it.getStringExtra(
                        EditJetpackSocialShareMessageActivity.RESULT_UPDATED_SHARE_MESSAGE
                    )
                    getEditorJetpackSocialViewModel().onJetpackSocialShareMessageChanged(shareMessage)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        prepublishingBottomSheetListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.post_prepublishing_bottom_sheet, container)
        keyboardResizeViewUtil = KeyboardResizeViewUtil(requireActivity(), view)
        keyboardResizeViewUtil.enable()
        return view
    }

    override fun onResume() {
        super.onResume()
        getEditorJetpackSocialViewModel().onResume(JetpackSocialFlow.PRE_PUBLISHING)
        /**
         * The back button normally closes the bottom sheet so now instead of doing that it goes back to
         * the home screen with the actions and only if pressed again will it close the bottom sheet.
         */
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action != KeyEvent.ACTION_DOWN) {
                    true
                } else {
                    viewModel.onDeviceBackPressed()
                    true
                }
            } else {
                false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PostPrepublishingBottomSheetBinding.bind(view)) {
            initViewModel(savedInstanceState)
            dialog?.setOnShowListener { dialogInterface ->
                val sheetDialog = dialogInterface as? BottomSheetDialog

                val bottomSheet = sheetDialog?.findViewById<View>(
                    MaterialR.id.design_bottom_sheet
                ) as? FrameLayout

                bottomSheet?.let {
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.skipCollapsed = true
                }
            }
            setupMinimumHeightForFragmentContainer()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        analyticsTrackerWrapper.track(Stat.PREPUBLISHING_BOTTOM_SHEET_DISMISSED)
        super.onDismiss(dialog)
    }

    private fun PostPrepublishingBottomSheetBinding.setupMinimumHeightForFragmentContainer() {
        val isPage = checkNotNull(arguments?.getBoolean(IS_PAGE)) {
            "arguments can't be null."
        }

        if (isPage) {
            prepublishingContentFragment.minimumHeight =
                resources.getDimensionPixelSize(R.dimen.prepublishing_fragment_container_min_height_for_page)
        } else {
            prepublishingContentFragment.minimumHeight =
                resources.getDimensionPixelSize(R.dimen.prepublishing_fragment_container_min_height)
        }
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this, viewModelFactory)[PrepublishingViewModel::class.java]

        viewModel.navigationTarget.observeEvent(this) { navigationState ->
            navigateToScreen(navigationState)
        }

        viewModel.dismissBottomSheet.observeEvent(this) {
            dismiss()
        }

        viewModel.triggerOnSubmitButtonClickedListener.observeEvent(this) { publishPost ->
            prepublishingBottomSheetListener?.onSubmitButtonClicked(publishPost)
        }

        viewModel.dismissKeyboard.observeEvent(this) {
            ActivityUtils.hideKeyboardForced(view)
        }

        viewModel.navigateToSharingSettings.observeEvent(this) { site ->
            context?.let { ActivityLauncher.viewBlogSharing(it, site) }
        }

        val prepublishingScreenState = savedInstanceState?.getParcelableCompat<PrepublishingScreen>(
            KEY_SCREEN_STATE
        )
        val site = requireNotNull(arguments?.getSerializableCompat<SiteModel>(SITE))
        viewModel.start(site, prepublishingScreenState)
    }

    private fun navigateToScreen(navigationTarget: PrepublishingNavigationTarget) {
        val (fragment, tag) = when (navigationTarget.targetScreen) {
            HOME -> {
                Pair(
                    PrepublishingHomeFragment.newInstance(),
                    PrepublishingHomeFragment.TAG
                )
            }

            PUBLISH -> Pair(
                PrepublishingPublishSettingsFragment.newInstance(),
                PrepublishingPublishSettingsFragment.TAG
            )

            TAGS -> {
                Pair(
                    PrepublishingTagsFragment.newInstance(navigationTarget.site),
                    PrepublishingTagsFragment.TAG
                )
            }

            CATEGORIES -> {
                Pair(
                    PrepublishingCategoriesFragment.newInstance(
                        navigationTarget.site,
                        navigationTarget.bundle
                    ),
                    PrepublishingCategoriesFragment.TAG
                )
            }

            ADD_CATEGORY -> {
                Pair(
                    PrepublishingAddCategoryFragment.newInstance(
                        navigationTarget.site,
                        navigationTarget.bundle
                    ),
                    PrepublishingAddCategoryFragment.TAG
                )
            }

            SOCIAL -> {
                Pair(
                    PrepublishingSocialFragment.newInstance(),
                    PrepublishingSocialFragment.TAG
                )
            }
        }

        fadeInFragment(fragment, tag)
    }

    private fun fadeInFragment(fragment: Fragment, tag: String) {
        childFragmentManager.let { fragmentManager ->
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentManager.findFragmentById(R.id.prepublishing_content_fragment)?.run {
                fragmentTransaction.addToBackStack(null).setCustomAnimations(
                    R.anim.prepublishing_fragment_fade_in,
                    R.anim.prepublishing_fragment_fade_out,
                    R.anim.prepublishing_fragment_fade_in,
                    R.anim.prepublishing_fragment_fade_out
                )
            }
            fragmentTransaction.replace(R.id.prepublishing_content_fragment, fragment, tag)
            fragmentTransaction.commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    override fun onBackClicked(bundle: Bundle?) {
        viewModel.onBackClicked(bundle)
    }

    override fun onActionClicked(actionType: ActionType, bundle: Bundle?) {
        viewModel.onActionClicked(actionType, bundle)
    }

    override fun onSubmitButtonClicked(publishPost: PublishPost) {
        viewModel.onSubmitButtonClicked(publishPost)
    }

    override fun getEditorJetpackSocialViewModel(): EditorJetpackSocialViewModel {
        jetpackSocialViewModel?.let {
            return it
        } ?: run {
            val viewModel = ViewModelProvider(
                requireActivity(), // try using the activity-scoped ViewModel, but only if it's started
                viewModelFactory
            )[EditorJetpackSocialViewModel::class.java].takeIf {
                it.isStarted
            } ?: run {
                ViewModelProvider(
                    this, // if not, let's use our own scope to create the ViewModel and start it
                    viewModelFactory
                )[EditorJetpackSocialViewModel::class.java].also {
                    val hook = getEditorHook()
                    it.start(hook.site, hook.editPostRepository)
                    // let's also handle the action events
                    it.actionEvents.observe(viewLifecycleOwner) { actionEvent ->
                        when (actionEvent) {
                            is OpenEditShareMessage -> {
                                val intent = createIntent(
                                    requireActivity(),
                                    actionEvent.shareMessage
                                )
                                editShareMessageActivityResultLauncher.launch(intent)
                            }

                            is OpenSocialConnectionsList -> {
                                ActivityLauncher.viewBlogSharing(requireActivity(), actionEvent.siteModel)
                            }

                            is OpenSubscribeJetpackSocial -> {
                                WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(
                                    requireActivity(),
                                    actionEvent.url
                                )
                            }
                        }
                    }
                }
            }

            return viewModel.also { jetpackSocialViewModel = it }
        }
    }

    @Suppress("TooGenericExceptionThrown")
    private fun getEditorHook(): EditPostActivityHook {
        val activity = activity
        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    companion object {
        const val TAG = "prepublishing_bottom_sheet_fragment_tag"
        const val SITE = "prepublishing_bottom_sheet_site_model"
        const val IS_PAGE = "prepublishing_bottom_sheet_is_page"

        @JvmStatic
        fun newInstance(site: SiteModel, isPage: Boolean) =
            PrepublishingBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(SITE, site)
                    putBoolean(IS_PAGE, isPage)
                }
            }
    }
}
