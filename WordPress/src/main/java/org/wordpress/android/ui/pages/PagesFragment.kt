package org.wordpress.android.ui.pages

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.pages_fragment.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.PageListFragment.Companion.Type
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.LOADING_MORE
import org.wordpress.android.viewmodel.pages.PagesViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PagesFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagesViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var actionMenuItem: MenuItem

    companion object {
        fun newInstance(): PagesFragment {
            return PagesFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val site = if (savedInstanceState == null) {
            activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
        }

        val nonNullActivity = checkNotNull(activity)
        val nonNullSite = checkNotNull(site)

        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(nonNullActivity)
        initializeViewModels(nonNullActivity, nonNullSite)
    }

    private fun initializeViews(activity: FragmentActivity) {
        pagesPager.adapter = PagesPagerAdapter(activity, childFragmentManager)
        tabLayout.setupWithViewPager(pagesPager)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.refresh()
        }

        newPageButton.setOnClickListener {}
    }

    private fun initializeSearchView() {
        searchRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        searchRecyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))

        actionMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return viewModel.onSearchExpanded()
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                return viewModel.onSearchCollapsed()
            }
        })

        val searchView = actionMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return viewModel.onSearchTextSubmit(query)
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return viewModel.onSearchTextChange(newText)
            }
        })

        // fix the search view margins to match the action bar
        val searchEditFrame = actionMenuItem.actionView.findViewById<LinearLayout>(R.id.search_edit_frame)
        (searchEditFrame.layoutParams as LinearLayout.LayoutParams)
                .apply { this.leftMargin = DisplayUtils.dpToPx(activity, -8) }
                .apply { this.rightMargin = DisplayUtils.dpToPx(activity, -12) }
    }

    private fun initializeViewModels(activity: FragmentActivity, site: SiteModel) {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get<PagesViewModel>(PagesViewModel::class.java)

        viewModel = ViewModelProviders.of(activity, viewModelFactory).get<PagesViewModel>(PagesViewModel::class.java)

        setupObservers()

        viewModel.start(site)
    }

    private fun setupObservers() {
        viewModel.searchResult.observe(this, Observer { result ->
            result?.let { setSearchResult(result) }
        })

        viewModel.isSearchExpanded.observe(this, Observer {
            if (it == true) {
                showSearchList(actionMenuItem)
            } else {
                hideSearchList(actionMenuItem)
            }
        })

        viewModel.listState.observe(this, Observer {
            refreshProgressBars(it)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_search, menu)
        actionMenuItem = checkNotNull(menu.findItem(R.id.action_search)) {
            "Menu does not contain mandatory search item"
        }

        initializeSearchView()
    }

    private fun refreshProgressBars(listState: PageListState?) {
        if (!isAdded || view == null) {
            return
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        swipeToRefreshHelper.isRefreshing = listState == FETCHING
        // We want to show the progress bar at the bottom while loading more but not for initial fetch
        val showLoadMore = listState == LOADING_MORE
        pagesListProgress.visibility = if (showLoadMore) View.VISIBLE else View.GONE
    }

    private fun hideSearchList(myActionMenuItem: MenuItem) {
        pagesPager.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        searchRecyclerView.visibility = View.GONE
        if (myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.collapseActionView()
        }
        launch(UI) {
            delay(500)
            newPageButton?.show()
        }
    }

    private fun showSearchList(myActionMenuItem: MenuItem) {
        pagesPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        searchRecyclerView.visibility = View.VISIBLE
        if (!myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.expandActionView()
        }
        newPageButton?.hide()
    }

    private fun setSearchResult(pages: List<PageItem>) {
        val adapter: PagesAdapter
        if (searchRecyclerView.adapter == null) {
            adapter = PagesAdapter { action, pageItem -> viewModel.onAction(action, pageItem) }
            searchRecyclerView.adapter = adapter
        } else {
            adapter = searchRecyclerView.adapter as PagesAdapter
        }
        adapter.update(pages)
    }
}

class PagesPagerAdapter(val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {
    companion object {
        const val PAGE_TABS = 4
    }

    override fun getCount(): Int = PAGE_TABS

    override fun getItem(position: Int): Fragment {
        return PageListFragment.newInstance(Type.getType(position))
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return Type.getType(position).text.let { context.getString(it) }
    }
}
