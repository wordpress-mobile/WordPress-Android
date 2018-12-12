package org.wordpress.android.ui.sitecreation.domain

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.LayoutRes
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.NewSiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.NewSiteCreationListener
import org.wordpress.android.ui.sitecreation.SearchInputWithHeader
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState
import javax.inject.Inject

private const val KEY_LIST_STATE = "list_state"

class NewSiteCreationDomainsFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private lateinit var nonNullActivity: FragmentActivity
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: NewSiteCreationDomainsViewModel

    private lateinit var searchInputWithHeader: SearchInputWithHeader

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_domains_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        searchInputWithHeader = SearchInputWithHeader(
                rootView = rootView,
                onClear = { viewModel.onClearTextBtnClicked() }
        )
        initRecyclerView(rootView)
        initViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = activity!!
        (nonNullActivity.application as WordPress).component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_LIST_STATE, linearLayoutManager.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            linearLayoutManager.onRestoreInstanceState(it)
        }
        // we need to set the `onTextChanged` after the viewState has been restored otherwise the viewModel.updateQuery
        // is called when the system sets the restored value to the EditText which results in an unnecessary request
        searchInputWithHeader.onTextChanged = { viewModel.updateQuery(it) }
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = NewSiteCreationDomainsAdapter()
        recyclerView.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(NewSiteCreationDomainsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                searchInputWithHeader.updateHeader(uiState.headerUiState)
                searchInputWithHeader.updateSearchInput(uiState.searchInputUiState)
                updateSuggestions(uiState.items)
            }
        })
        viewModel.clearBtnClicked.observe(this, Observer {
            searchInputWithHeader.setInputText("")
        })

        viewModel.domainSelected.observe(this, Observer { domainName ->
            domainName?.let {
                // TODO: Add a listener for domain selected
            }
        })
        viewModel.start()
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    private fun updateSuggestions(suggestions: List<DomainsListItemUiState>) {
        (recyclerView.adapter as NewSiteCreationDomainsAdapter).update(suggestions)
    }

    override fun getScreenTitle(): String {
        val arguments = arguments
        if (arguments == null || !arguments.containsKey(EXTRA_SCREEN_TITLE)) {
            throw IllegalStateException("Required argument screen title is missing.")
        }
        return arguments.getString(EXTRA_SCREEN_TITLE)
    }

    companion object {
        const val TAG = "site_creation_domains_fragment_tag"

        fun newInstance(screenTitle: String): NewSiteCreationDomainsFragment {
            val fragment = NewSiteCreationDomainsFragment()
            val bundle = Bundle()
            bundle.putString(NewSiteCreationBaseFormFragment.EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
