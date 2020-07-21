package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_post_settings_tags.*
import kotlinx.android.synthetic.main.prepublishing_toolbar.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PrepublishingTagsFragment : TagsFragment(), TagsSelectedListener {
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

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
        @JvmStatic fun newInstance(site: SiteModel): PrepublishingTagsFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
            }
            return PrepublishingTagsFragment().apply { arguments = bundle }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        back_button.setOnClickListener {
            trackTagsChangedEvent()
            viewModel.onBackButtonClicked()
        }
        initViewModel()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun trackTagsChangedEvent() {
        if (wereTagsChanged()) {
            analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_TAGS_CHANGED)
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingTagsViewModel::class.java)

        viewModel.dismissBottomSheet.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onCloseClicked()
            }
        })

        viewModel.dismissKeyboard.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                ActivityUtils.hideKeyboardForced(tags_edit_text)
            }
        })

        viewModel.navigateToHomeScreen.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onBackClicked()
            }
        })

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, Observer { uiString ->
            toolbar_title.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.start(getEditPostRepository())
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) { "This is possibly null because it's " +
                "called during config changes." }

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
