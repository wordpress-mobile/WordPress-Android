package org.wordpress.android.ui.pages

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.pages_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PagesViewModel
import org.wordpress.android.viewmodel.pages.SearchListViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class SearchListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SearchListViewModel
    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        fun newInstance(): SearchListFragment {
            return SearchListFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }
        super.onSaveInstanceState(outState)
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val pagesViewModel = ViewModelProviders.of(activity, viewModelFactory).get(PagesViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchListViewModel::class.java)
        viewModel.start(pagesViewModel)

        setupObservers()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))
    }

    private fun setupObservers() {
        viewModel.searchResult.observe(this, Observer { data ->
            data?.let { setSearchResult(data) }
        })
    }

    private fun setSearchResult(pages: List<PageItem>) {
        val adapter: PageSearchAdapter
        if (recyclerView.adapter == null) {
            adapter = PageSearchAdapter(
                    { action, page -> viewModel.onMenuAction(action, page) },
                    { page -> viewModel.onItemTapped(page) }
            )
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PageSearchAdapter
        }
        adapter.update(pages)
    }
}
