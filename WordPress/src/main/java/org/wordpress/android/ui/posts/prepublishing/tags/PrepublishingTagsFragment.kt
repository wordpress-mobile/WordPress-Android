package org.wordpress.android.ui.posts.prepublishing.tags

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.PrepublishingTagsFragmentBinding
import org.wordpress.android.databinding.PrepublishingToolbarBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingScreenClosedListener
import org.wordpress.android.ui.posts.trackPrepublishingNudges
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingTagsFragment : Fragment(R.layout.prepublishing_tags_fragment) {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var taxonomyStore: TaxonomyStore

    private lateinit var viewModel: PrepublishingTagsViewModel
    private lateinit var site: SiteModel
    private var closeListener: PrepublishingScreenClosedListener? = null
    private var binding: PrepublishingTagsFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        site = requireNotNull(arguments?.getSerializableCompat<SiteModel>(WordPress.SITE)) {
            "Required argument site is missing."
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PrepublishingTagsFragmentBinding.bind(view)) {
            binding = this
            includePrepublishingToolbar.init()
            initViewModel()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun PrepublishingToolbarBinding.init() {
        toolbarTitle.text = getString(R.string.prepublishing_nudges_toolbar_title_tags)
        backButton.setOnClickListener {
            viewModel.commitPendingTag()
            if (viewModel.wereTagsChanged()) {
                analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_TAGS_CHANGED)
            }
            viewModel.onBackButtonClicked()
        }
    }

    private fun PrepublishingTagsFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(this@PrepublishingTagsFragment, viewModelFactory)
            .get(PrepublishingTagsViewModel::class.java)

        viewModel.dismissKeyboard.observeEvent(viewLifecycleOwner) {
            ActivityUtils.hideKeyboardForced(requireView())
        }

        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner) {
            closeListener?.onBackClicked()
        }

        prepublishingTagsComposeView.setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.observeAsState(PrepublishingTagsUiState())
                PrepublishingTagsScreen(
                    uiState = uiState,
                    onInputChanged = viewModel::onInputChanged,
                    onTagAdded = viewModel::onTagAdded,
                    onTagRemoved = viewModel::onTagRemoved,
                    onLastTagRemoved = viewModel::onLastTagRemoved,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        viewModel.start(getEditPostRepository(), getSiteTagNames())
    }

    private fun getSiteTagNames(): List<String> =
        taxonomyStore.getTagsForSite(site).map { StringEscapeUtils.unescapeHtml4(it.name) }

    private fun getEditPostRepository(): EditPostRepository {
        val editorDataProvider = requireNotNull(getEditorDataProvider()) {
            "This is possibly null because it's called during config changes."
        }
        return editorDataProvider.editPostRepository
    }

    private fun getEditorDataProvider(): EditPostSettingsFragment.EditorDataProvider? {
        val activity = activity ?: return null
        return if (activity is EditPostSettingsFragment.EditorDataProvider) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditorDataProvider")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.causeOfChange == TaxonomyAction.FETCH_TAGS) {
            viewModel.onSiteTagsChanged(getSiteTagNames())
        }
    }

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
}
