package org.wordpress.android.ui.sitecreation.verticals

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.site_creation_error_with_retry.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.OnSkipClickedListener
import org.wordpress.android.ui.sitecreation.misc.SearchInputWithHeader
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenProgressUiState
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject
import kotlin.properties.Delegates

class SiteCreationVerticalsFragment : SiteCreationBaseFormFragment() {
    private lateinit var nonNullActivity: FragmentActivity
    private var segmentId by Delegates.notNull<Long>()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: SiteCreationVerticalsViewModel

    private lateinit var fullscreenErrorLayout: ViewGroup
    private lateinit var fullscreenProgressLayout: ViewGroup
    private lateinit var contentLayout: ViewGroup
    private lateinit var errorLayout: ViewGroup
    private lateinit var skipButton: Button
    private lateinit var searchInputWithHeader: SearchInputWithHeader

    private lateinit var verticalsScreenListener: VerticalsScreenListener
    private lateinit var helpClickedListener: OnHelpClickedListener
    private lateinit var skipClickedListener: OnSkipClickedListener

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is VerticalsScreenListener) {
            throw IllegalStateException("Parent activity must implement VerticalsScreenListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
        if (context !is OnSkipClickedListener) {
            throw IllegalStateException("Parent activity must implement OnSkipClickedListener.")
        }
        verticalsScreenListener = context
        helpClickedListener = context
        skipClickedListener = context
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_verticals_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        fullscreenErrorLayout = rootView.findViewById(R.id.error_layout)
        fullscreenProgressLayout = rootView.findViewById(R.id.progress_layout)
        contentLayout = rootView.findViewById(R.id.content_layout)

        errorLayout = rootView.findViewById(R.id.error_layout)
        searchInputWithHeader = SearchInputWithHeader(
                uiHelpers = uiHelpers,
                rootView = rootView,
                onClear = { viewModel.onClearTextBtnClicked() }
        )
        initRecyclerView(rootView)
        initRetryButton(rootView)
        initSkipButton(rootView)
        initViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = activity!!
        (nonNullActivity.application as WordPress).component().inject(this)
        segmentId = arguments?.getLong(EXTRA_SEGMENT_ID, -1L) ?: -1L
        if (segmentId == -1L) {
            throw IllegalStateException("SegmentId is required.")
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // we need to set the `onTextChanged` after the viewState has been restored otherwise the viewModel.updateQuery
        // is called when the system sets the restored value to the EditText which results in an unnecessary request
        searchInputWithHeader.onTextChanged = { viewModel.updateQuery(it) }
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = SiteCreationVerticalsAdapter()
        recyclerView.adapter = adapter
    }

    private fun initRetryButton(rootView: ViewGroup) {
        val retryBtn = rootView.findViewById<Button>(R.id.error_retry)
        retryBtn.setOnClickListener { viewModel.onFetchSegmentsPromptRetry() }
    }

    private fun initSkipButton(rootView: ViewGroup) {
        skipButton = rootView.findViewById(R.id.btn_skip)
        skipButton.setOnClickListener { viewModel.onSkipStepBtnClicked() }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SiteCreationVerticalsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                uiHelpers.updateVisibility(fullscreenProgressLayout, uiState.fullscreenProgressLayoutVisibility)
                uiHelpers.updateVisibility(contentLayout, uiState.contentLayoutVisibility)
                uiHelpers.updateVisibility(fullscreenErrorLayout, uiState.fullscreenErrorLayoutVisibility)

                when (uiState) {
                    is VerticalsContentUiState -> updateContentLayout(uiState)
                    is VerticalsFullscreenProgressUiState -> { // no action
                    }
                    is VerticalsFullscreenErrorUiState -> updateErrorLayout(errorLayout, uiState)
                }
            }
        })
        viewModel.clearBtnClicked.observe(this, Observer {
            searchInputWithHeader.setInputText("")
        })

        viewModel.verticalSelected.observe(this, Observer { verticalId ->
            verticalId?.let { verticalsScreenListener.onVerticalSelected(verticalId) }
        })
        viewModel.skipBtnClicked.observe(this, Observer { skipClickedListener.onSkipClicked() })
        viewModel.onHelpClicked.observe(this, Observer {
            helpClickedListener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_VERTICALS)
        })
        viewModel.start(segmentId)
    }

    private fun updateContentLayout(uiState: VerticalsContentUiState) {
        uiHelpers.updateVisibility(skipButton, uiState.showSkipButton)
        searchInputWithHeader.updateHeader(nonNullActivity, uiState.headerUiState)
        searchInputWithHeader.updateSearchInput(nonNullActivity, uiState.searchInputUiState)
        updateSuggestions(uiState.items)
    }

    private fun updateErrorLayout(errorLayout: ViewGroup, errorUiStateState: VerticalsFullscreenErrorUiState) {
        uiHelpers.setTextOrHide(errorLayout.error_title, errorUiStateState.titleResId)
        uiHelpers.setTextOrHide(errorLayout.error_subtitle, errorUiStateState.subtitleResId)
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    private fun updateSuggestions(suggestions: List<VerticalsListItemUiState>) {
        if (suggestions.isNotEmpty()) {
            view?.announceForAccessibility(getString(R.string.suggestions_updated_content_description))
        }
        (recyclerView.adapter as SiteCreationVerticalsAdapter).update(suggestions)
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
    }

    companion object {
        const val TAG = "site_creation_verticals_fragment_tag"
        private const val EXTRA_SEGMENT_ID = "extra_segment_id"

        fun newInstance(screenTitle: String, segmentId: Long): SiteCreationVerticalsFragment {
            val fragment = SiteCreationVerticalsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            bundle.putLong(EXTRA_SEGMENT_ID, segmentId)
            fragment.arguments = bundle
            return fragment
        }
    }
}
