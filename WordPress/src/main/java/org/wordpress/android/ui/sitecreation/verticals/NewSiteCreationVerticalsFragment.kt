package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.LayoutRes
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.NewSiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.NewSiteCreationListener
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsContentState.CONTENT
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsContentState.FULLSCREEN_ERROR
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsContentState.FULLSCREEN_PROGRESS
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsSearchInputUiState
import javax.inject.Inject
import kotlin.properties.Delegates

private const val keyListState = "list_state"

class NewSiteCreationVerticalsFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private lateinit var nonNullActivity: FragmentActivity
    private var segmentId by Delegates.notNull<Long>()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

    private lateinit var fullscreenErrorLayout: ViewGroup
    private lateinit var fullscreenProgressLayout: ViewGroup
    private lateinit var contentLayout: ViewGroup
    private lateinit var skipButton: Button

    private lateinit var headerLayout: ViewGroup
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var searchEditText: EditText
    private lateinit var searchEditTextProgressBar: View
    private lateinit var clearAllButton: View

    @Inject protected lateinit var viewModelFactory: ViewModelProvider.Factory

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_verticals_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // TODO receive title from the MainVM
        // important for accessibility - talkback
        nonNullActivity.setTitle(R.string.new_site_creation_verticals_title)
        fullscreenErrorLayout = rootView.findViewById(R.id.error_layout)
        fullscreenProgressLayout = rootView.findViewById(R.id.progress_layout)
        contentLayout = rootView.findViewById(R.id.content_layout)
        initSearchEditText(rootView)
        initHeader(rootView)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(keyListState, linearLayoutManager.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Parcelable>(keyListState)?.let {
            linearLayoutManager.onRestoreInstanceState(it)
        }
        initTextWatcher()
    }

    private fun initHeader(rootView: ViewGroup) {
        headerLayout = rootView.findViewById(R.id.header_layout)
        headerTitle = headerLayout.findViewById(R.id.title)
        headerSubtitle = headerLayout.findViewById(R.id.subtitle)
    }

    private fun initSearchEditText(rootView: ViewGroup) {
        searchEditText = rootView.findViewById(R.id.input)
        searchEditTextProgressBar = rootView.findViewById(R.id.progress_bar)
        val drawable = AppCompatResources.getDrawable(nonNullActivity, R.drawable.ic_search_white_24dp)
        drawable?.setTint(ContextCompat.getColor(nonNullActivity, R.color.grey))
        searchEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        initClearTextButton(rootView)
    }

    private fun initClearTextButton(rootView: ViewGroup) {
        clearAllButton = rootView.findViewById(R.id.clear_all_btn)
        val drawable = AppCompatResources.getDrawable(nonNullActivity, R.drawable.ic_close_white_24dp)
        drawable?.setTint(ContextCompat.getColor(nonNullActivity, R.color.grey))
        clearAllButton.background = drawable

        val clearAllLayout = rootView.findViewById<View>(R.id.clear_all_layout)
        clearAllLayout.setOnClickListener {
            viewModel.onClearTextBtnClicked()
        }
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = NewSiteCreationVerticalsAdapter()
        recyclerView.adapter = adapter
    }

    private fun initRetryButton(rootView: ViewGroup) {
        val retryBtn = rootView.findViewById<Button>(R.id.error_retry)
        retryBtn.setOnClickListener { _ -> viewModel.onFetchSegmentsPromptRetry() }
    }

    private fun initSkipButton(rootView: ViewGroup) {
        skipButton = rootView.findViewById(R.id.btn_skip)
        skipButton.setOnClickListener { _ ->
            // TODO add skip action
        }
    }

    private fun initTextWatcher() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateQuery(s?.toString() ?: "")
            }
        })
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(NewSiteCreationVerticalsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { state ->
            state?.let {
                updateVisibility(contentLayout, state.contentState == CONTENT)
                updateVisibility(fullscreenErrorLayout, state.contentState == FULLSCREEN_ERROR)
                updateVisibility(fullscreenProgressLayout, state.contentState == FULLSCREEN_PROGRESS)
                updateVisibility(skipButton, state.showSkipButton)
                updateHeader(state.headerUiState)
                updateSearchInput(state.searchInputState)
                updateSuggestions(state.items)
            }
        })
        viewModel.clearBtnClicked.observe(this, Observer {
            searchEditText.setText("")
        })

        viewModel.start(segmentId)
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    private fun updateHeader(uiState: VerticalsHeaderUiState) {
        if (!uiState.isVisible && headerLayout.visibility == View.VISIBLE) {
            headerLayout.animate()
                    .translationY(-headerLayout.height.toFloat())
        } else if (uiState.isVisible && headerLayout.visibility == View.GONE) {
            headerLayout.animate()
                    .translationY(0f)
        }
        updateVisibility(headerLayout, uiState.isVisible)
        headerTitle.text = uiState.title
        headerSubtitle.text = uiState.subtitle
    }

    private fun updateSearchInput(uiState: VerticalsSearchInputUiState) {
        searchEditText.hint = uiState.hint
        updateVisibility(searchEditTextProgressBar, uiState.showProgress)
        updateVisibility(clearAllButton, uiState.showClearButton)
    }

    private fun updateSuggestions(suggestions: List<VerticalsListItemUiState>) {
        (recyclerView.adapter as NewSiteCreationVerticalsAdapter).update(suggestions)
    }

    private fun updateVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    companion object {
        val TAG = "site_creation_verticals_fragment_tag"
        private val EXTRA_SEGMENT_ID = "extra_segment_id"

        fun newInstance(segmentId: Long): NewSiteCreationVerticalsFragment {
            val fragment = NewSiteCreationVerticalsFragment()
            val bundle = Bundle()
            bundle.putLong(EXTRA_SEGMENT_ID, segmentId)
            fragment.arguments = bundle
            return fragment
        }
    }
}
