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
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.IListViewModel
import org.wordpress.android.viewmodel.pages.IListViewModel.ListType
import org.wordpress.android.viewmodel.pages.IListViewModel.ListType.SEARCH
import org.wordpress.android.viewmodel.pages.PageListViewModel
import org.wordpress.android.viewmodel.pages.PagesViewModel
import org.wordpress.android.viewmodel.pages.SearchListViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PageListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: IListViewModel
    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"
        private const val statusKey = "status_key"

        fun newPageListInstance(pageStatus: PageStatus): PageListFragment {
            val fragment = PageListFragment()
            val bundle = Bundle()
            bundle.putSerializable(statusKey, pageStatus)
            bundle.putSerializable(typeKey, ListType.PAGES)
            fragment.arguments = bundle
            return fragment
        }

        fun newSearchListInstance(): PageListFragment {
            val fragment = PageListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, ListType.SEARCH)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)
        val type = checkNotNull(arguments?.getSerializable(typeKey) as ListType?)

        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, type)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }
        super.onSaveInstanceState(outState)
    }

    private fun initializeViewModels(activity: FragmentActivity, type: ListType) {
        val pagesViewModel = ViewModelProviders.of(activity, viewModelFactory).get(PagesViewModel::class.java)

        if (type == SEARCH) {
            val searchListViewModel = ViewModelProviders.of(this, viewModelFactory)
                    .get(SearchListViewModel::class.java)

            viewModel = searchListViewModel
            searchListViewModel.start(pagesViewModel)
        } else {
            val pageStatus = checkNotNull(arguments?.getSerializable(statusKey) as PageStatus?)
            val pageListViewModel = ViewModelProviders.of(this, viewModelFactory)
                    .get(pageStatus.name, PageListViewModel::class.java)

            viewModel = pageListViewModel
            pageListViewModel.start(pageStatus, pagesViewModel)
        }

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
        viewModel.pages.observe(this, Observer { data ->
            data?.let { setPages(data) }
        })
    }

    private fun setPages(pages: List<PageItem>) {
        val adapter: PagesAdapter
        if (recyclerView.adapter == null) {
            if (viewModel is PageListViewModel) {
                adapter = PagesAdapter(
                        { action, page -> viewModel.onMenuAction(action, page) },
                        { page -> viewModel.onItemTapped(page) },
                        { (viewModel as PageListViewModel).onEmptyListNewPageButtonTapped() })
            } else {
                adapter = PagesAdapter(
                        { action, page -> viewModel.onMenuAction(action, page) },
                        { page -> viewModel.onItemTapped(page) })
            }
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PagesAdapter
        }
        adapter.update(pages)
    }
}
