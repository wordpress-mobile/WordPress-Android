package org.wordpress.android.ui.pages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.page_parent_fragment.*
import kotlinx.android.synthetic.main.pages_list_fragment.recyclerView
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PageParentViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject
import javax.inject.Named

class PageParentFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @field:[Inject Named(UI_SCOPE)] lateinit var uiScope: CoroutineScope
    private lateinit var viewModel: PageParentViewModel

    private val listStateKey = "list_state"

    private var linearLayoutManager: LinearLayoutManager? = null
    private var saveButton: MenuItem? = null
    private lateinit var searchAction: MenuItem

    private var pageId: Long? = null
    private var restorePreviousSearch = false

    companion object {
        fun newInstance(): PageParentFragment {
            return PageParentFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
            return true
        } else if (item.itemId == R.id.save_parent) {
            viewModel.onSaveButtonTapped()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun returnParentChoiceAndExit() {
        val result = Intent()
        result.putExtra(EXTRA_PAGE_REMOTE_ID_KEY, pageId)
        result.putExtra(EXTRA_PAGE_PARENT_ID_KEY, viewModel.currentParent.id)
        activity?.setResult(Activity.RESULT_OK, result)
        activity?.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.page_parent_menu, menu)

        saveButton = menu.findItem(R.id.save_parent)
        viewModel.isSaveButtonVisible.value?.let { saveButton?.isVisible = it }
        searchAction = checkNotNull(menu.findItem(R.id.action_search)) {
            "Menu does not contain mandatory search item"
        }

        initializeSearchView()
    }

    private fun initializeSearchView() {
        searchAction.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.onSearchExpanded(restorePreviousSearch)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.onSearchCollapsed()
                return true
            }
        })

        val searchView = searchAction.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.onSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (restorePreviousSearch) {
                    restorePreviousSearch = false
                    searchView.setQuery(viewModel.lastSearchQuery, false)
                } else {
                    viewModel.onSearch(newText)
                }
                return true
            }
        })

        val searchEditFrame = searchAction?.actionView.findViewById<LinearLayout>(R.id.search_edit_frame)
        (searchEditFrame.layoutParams as LinearLayout.LayoutParams)
                .apply { this.leftMargin = DisplayUtils.dpToPx(activity, -8) }

        viewModel.isSearchExpanded.observe(this, Observer {
            if (it == true) {
                showSearchList(searchAction)
            } else {
                hideSearchList(searchAction)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_parent_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageId = activity?.intent?.getLongExtra(EXTRA_PAGE_REMOTE_ID_KEY, 0)

        val nonNullPageId = checkNotNull(pageId)
        val nonNullActivity = checkNotNull(activity)

        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(nonNullActivity, savedInstanceState)
        initializeViewModels(nonNullActivity, nonNullPageId, savedInstanceState == null)
    }

    private fun initializeViews(activity: FragmentActivity, savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))

        val searchFragment = PageParentSearchFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameSearch, searchFragment)
                .commit()
    }

    private fun initializeViewModels(
        activity: FragmentActivity,
        pageId: Long,
        isFirstStart: Boolean
    ) {
        viewModel = ViewModelProviders.of(activity, viewModelFactory)
                .get(PageParentViewModel::class.java)

        setupObservers()

        if (isFirstStart) {
            val site = activity.intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
            val nonNullSite = checkNotNull(site)
            viewModel.start(nonNullSite, pageId)
        } else {
            restorePreviousSearch = true
        }
    }

    private fun setupObservers() {
        viewModel.pages.observe(viewLifecycleOwner, Observer { pages ->
            pages?.let { setPages(pages) }
        })

        viewModel.isSaveButtonVisible.observe(viewLifecycleOwner, Observer { isVisible ->
            isVisible?.let { saveButton?.isVisible = isVisible }
        })

        viewModel.saveParent.observe(viewLifecycleOwner, Observer {
            returnParentChoiceAndExit()
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let { outState.putParcelable(listStateKey, it.onSaveInstanceState()) }
        super.onSaveInstanceState(outState)
    }

    private fun setPages(pages: List<PageItem>) {
        val adapter: PageParentAdapter
        if (recyclerView.adapter == null) {
            adapter = PageParentAdapter({ page -> viewModel.onParentSelected(page) }, uiScope)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PageParentAdapter
        }
        adapter.update(pages)
    }

    private fun hideSearchList(myActionMenuItem: MenuItem) {
        recyclerView.visibility = View.VISIBLE
        frameSearch.visibility = View.GONE
        if (myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.collapseActionView()
        }
        /**Force the recyclerview to redraw if selection has changed while in search mode*/
        if (viewModel.currentParent != viewModel.initialParent) {
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun showSearchList(myActionMenuItem: MenuItem) {
        frameSearch.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        if (!myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.expandActionView()
        }
    }
}
