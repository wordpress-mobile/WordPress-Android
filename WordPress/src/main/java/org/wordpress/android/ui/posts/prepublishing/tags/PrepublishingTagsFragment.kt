package org.wordpress.android.ui.posts.prepublishing.tags

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.PrepublishingTagsFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.TagsFragment
import org.wordpress.android.ui.posts.TagsSelectedListener
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingScreenClosedListener
import org.wordpress.android.ui.posts.trackPrepublishingNudges
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingTagsFragment : TagsFragment(), TagsSelectedListener {
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModel: PrepublishingTagsViewModel

    override fun getContentLayout() = R.layout.prepublishing_tags_fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
        mTagsSelectedListener = this
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun getTagsFromEditPostRepositoryOrArguments() = viewModel.getPostTags()

    companion object {
        const val TAG = "prepublishing_tags_fragment_tag"

        @JvmStatic
        fun newInstance(site: SiteModel): PrepublishingTagsFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
            }
            return PrepublishingTagsFragment().apply { arguments = bundle }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(PrepublishingTagsFragmentBinding.bind(view)) {
            prepublishingToolbar.backButton.setOnClickListener {
                trackTagsChangedEvent()
                viewModel.onBackButtonClicked()
            }
            initViewModel()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun trackTagsChangedEvent() {
        if (wereTagsChanged()) {
            analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_TAGS_CHANGED)
        }
    }

    private fun PrepublishingTagsFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(this@PrepublishingTagsFragment, viewModelFactory)
            .get(PrepublishingTagsViewModel::class.java)

        viewModel.dismissKeyboard.observeEvent(viewLifecycleOwner, {
            ActivityUtils.hideKeyboardForced(fragmentPostSettingsTags.tagsEditText)
        })

        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner, {
            closeListener?.onBackClicked()
        })

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, { uiString ->
            prepublishingToolbar.toolbarTitle.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.start(getEditPostRepository())
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "This is possibly null because it's " +
                    "called during config changes."
        }

        return editPostActivityHook.editPostRepository
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null
        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    override fun onTagsSelected(selectedTags: String) {
        viewModel.onTagsSelected(selectedTags)
    }
}
