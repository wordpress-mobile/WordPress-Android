package org.wordpress.android.ui.pages

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.pages_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.GutenbergWarningFragmentDialog.GutenbergWarningDialogClickInterface
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import org.wordpress.android.viewmodel.pages.PagesViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject

class PagesFragment : Fragment(), GutenbergWarningDialogClickInterface {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagesViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var actionMenuItem: MenuItem
    @Inject lateinit var postStore: PostStore

    private var restorePreviousSearch = false

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

        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(nonNullActivity)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.EDIT_POST && resultCode == Activity.RESULT_OK && data != null) {
            val pageId = data.getLongExtra(EditPostActivity.EXTRA_POST_REMOTE_ID, -1)
            if (pageId != -1L) {
                onPageEditFinished(pageId)
            }
        } else if (requestCode == RequestCodes.PAGE_PARENT && resultCode == Activity.RESULT_OK && data != null) {
            val parentId = data.getLongExtra(EXTRA_PAGE_PARENT_ID_KEY, -1)
            val pageId = data.getLongExtra(EXTRA_PAGE_REMOTE_ID_KEY, -1)
            if (pageId != -1L && parentId != -1L) {
                onPageParentSet(pageId, parentId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    fun onSpecificPageRequested(remotePageId: Long) {
        viewModel.onSpecificPageRequested(remotePageId)
    }

    private fun onPageEditFinished(pageId: Long) {
        viewModel.onPageEditFinished(pageId)
    }

    private fun onPageParentSet(pageId: Long, parentId: Long) {
        viewModel.onPageParentSet(pageId, parentId)
    }

    private fun initializeViews(activity: FragmentActivity) {
        pagesPager.adapter = PagesPagerAdapter(activity, childFragmentManager)
        tabLayout.setupWithViewPager(pagesPager)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) {
            viewModel.onPullToRefresh()
        }

        newPageButton.setOnClickListener {
            viewModel.onNewPageButtonTapped()
        }

        pagesPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                viewModel.onPageTypeChanged(PagesPagerAdapter.pageTypes[position])
            }
        })

        val searchFragment = SearchListFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.searchFrame, searchFragment)
                .commit()

        pagesPager.setOnTouchListener { _, event ->
            swipeToRefreshHelper.setEnabled(false)
            if (event.action == MotionEvent.ACTION_UP) {
                swipeToRefreshHelper.setEnabled(true)
            }
            return@setOnTouchListener false
        }
    }

    private fun initializeSearchView() {
        actionMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.onSearchExpanded(restorePreviousSearch)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.onSearchCollapsed()
                return true
            }
        })

        val searchView = actionMenuItem.actionView as SearchView
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

        // fix the search view margins to match the action bar
        val searchEditFrame = actionMenuItem.actionView.findViewById<LinearLayout>(R.id.search_edit_frame)
        (searchEditFrame.layoutParams as LinearLayout.LayoutParams)
                .apply { this.leftMargin = DisplayUtils.dpToPx(activity, -8) }

        viewModel.isSearchExpanded.observe(this, Observer {
            if (it == true) {
                showSearchList(actionMenuItem)
            } else {
                hideSearchList(actionMenuItem)
            }
        })
    }

    private fun initializeViewModels(activity: FragmentActivity, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(PagesViewModel::class.java)

        setupObservers(activity)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            restorePreviousSearch = true
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        val nonNullSite = checkNotNull(site)
        viewModel.start(nonNullSite)
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.listState.observe(this, Observer {
            refreshProgressBars(it)
        })

        viewModel.createNewPage.observe(this, Observer {
            ActivityLauncher.addNewPageForResult(this, viewModel.site)
        })

        viewModel.showSnackbarMessage.observe(this, Observer { holder ->
            val parent = activity.findViewById<View>(R.id.coordinatorLayout)
            if (holder != null && parent != null) {
                if (holder.buttonTitleRes == null) {
                    Snackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG).show()
                } else {
                    val snackbar = Snackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG)
                    snackbar.setAction(getString(holder.buttonTitleRes)) { _ -> holder.buttonAction() }
                    snackbar.show()
                }
            }
        })

        viewModel.editPage.observe(this, Observer { page ->
            page?.let {
                val post = postStore.getPostByLocalPostId(page.pageId)
                val isGutenbergContent = PostUtils.contentContainsGutenbergBlocks(post?.content)
                if (isGutenbergContent && !AppPrefs.isGutenbergWarningDialogDisabled()) {
                    PostUtils.showGutenbergCompatibilityWarningDialog(
                            getActivity(), fragmentManager, post, viewModel.site
                    )
                } else {
                    ActivityLauncher.editPageForResult(this, page)
                }
            }
        })

        viewModel.previewPage.observe(this, Observer { page ->
            page?.let { ActivityLauncher.viewPagePreview(this, page) }
        })

        viewModel.setPageParent.observe(this, Observer { page ->
            page?.let { ActivityLauncher.viewPageParentForResult(this, page) }
        })

        viewModel.displayDeleteDialog.observe(this, Observer { page ->
            page?.let { displayDeleteDialog(page) }
        })

        viewModel.isNewPageButtonVisible.observe(this, Observer { isVisible ->
            isVisible?.let {
                if (isVisible) {
                    newPageButton.show()
                } else {
                    newPageButton.hide()
                }
            }
        })

        viewModel.scrollToPage.observe(this, Observer { requestedPage ->
            requestedPage?.let { page ->
                val pagerIndex = PagesPagerAdapter.pageTypes.indexOf(PageListType.fromPageStatus(page.status))
                pagesPager.currentItem = pagerIndex
                (pagesPager.adapter as PagesPagerAdapter).scrollToPage(page)
            }
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

    fun onPageDeleteConfirmed(remoteId: Long) {
        viewModel.onDeleteConfirmed(remoteId)
    }

    override fun onGutenbergWarningDialogEditPostClicked(gutenbergRemotePageId: Long) {
        val post = postStore.getPostByRemotePostId(gutenbergRemotePageId, viewModel.site)
        // track event
        PostUtils.trackGutenbergDialogEvent(
                AnalyticsTracker.Stat.GUTENBERG_WARNING_CONFIRM_DIALOG_YES_TAPPED, post, viewModel.site
        )
        ActivityLauncher.editPostOrPageForResult(activity, viewModel.site, post)
    }

    override fun onGutenbergWarningDialogCancelClicked(gutenbergRemotePageId: Long) {
        val post = postStore.getPostByRemotePostId(gutenbergRemotePageId, viewModel.site)
        // guarding against null post as we only want to track here
        if (post != null) {
            // track event
            PostUtils.trackGutenbergDialogEvent(
                    AnalyticsTracker.Stat.GUTENBERG_WARNING_CONFIRM_DIALOG_CANCEL_TAPPED, post, viewModel.site
            )
        }
    }

    override fun onGutenbergWarningDialogLearnMoreLinkClicked(gutenbergRemotePageId: Long) {
        // here launch the web the Gutenberg Learn more
        val site = viewModel.site
        val urlToUse =
                if (site.isWPCom || site.isJetpackConnected) {
                    getString(R.string.dialog_gutenberg_compatibility_learn_more_url_wpcom)
                } else {
                    getString(R.string.dialog_gutenberg_compatibility_learn_more_url_wporg)
                }
        WPWebViewActivity.openURL(activity, urlToUse)

        // guarding against null post as we only want to track here
        val post = postStore.getPostByRemotePostId(gutenbergRemotePageId, viewModel.site)
        if (post != null) {
            // track event
            PostUtils.trackGutenbergDialogEvent(
                    AnalyticsTracker.Stat.GUTENBERG_WARNING_CONFIRM_DIALOG_LEARN_MORE_TAPPED,
                    post, viewModel.site
            )
        }
    }

    override fun onGutenbergWarningDialogDontShowAgainClicked(gutenbergRemotePageId: Long, checked: Boolean) {
        AppPrefs.setGutenbergWarningDialogDisabled(checked)
        val post = postStore.getPostByRemotePostId(gutenbergRemotePageId, viewModel.site)
        // guarding against null post as we only want to track here
        if (post != null) {
            // track event
            val trackValue =
                    if (checked) {
                        AnalyticsTracker.Stat.GUTENBERG_WARNING_CONFIRM_DIALOG_DONT_SHOW_AGAIN_CHECKED
                    } else {
                        AnalyticsTracker.Stat.GUTENBERG_WARNING_CONFIRM_DIALOG_DONT_SHOW_AGAIN_UNCHECKED
                    }
            PostUtils.trackGutenbergDialogEvent(
                    trackValue,
                    post, viewModel.site
            )
        }
    }

    private fun refreshProgressBars(listState: PageListState?) {
        if (!isAdded || view == null) {
            return
        }
        // We want to show the swipe refresher for the initial fetch but not while loading more
        swipeToRefreshHelper.isRefreshing = listState == FETCHING
    }

    private fun hideSearchList(myActionMenuItem: MenuItem) {
        pagesPager.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        searchFrame.visibility = View.GONE
        if (myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.collapseActionView()
        }
    }

    private fun showSearchList(myActionMenuItem: MenuItem) {
        pagesPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        searchFrame.visibility = View.VISIBLE
        if (!myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.expandActionView()
        }
    }

    private fun displayDeleteDialog(page: Page) {
        val dialog = BasicFragmentDialog()
        dialog.initialize(
                page.id.toString(),
                getString(string.delete_page),
                getString(string.page_delete_dialog_message, page.title),
                getString(string.delete),
                getString(string.cancel)
        )
        dialog.show(fragmentManager, page.id.toString())
    }
}

class PagesPagerAdapter(val context: Context, val fm: FragmentManager) : FragmentPagerAdapter(fm) {
    companion object {
        val pageTypes = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)
    }

    private val listFragments = mutableMapOf<PageListType, WeakReference<PageListFragment>>()

    override fun getCount(): Int = pageTypes.size

    override fun getItem(position: Int): Fragment {
        val fragment = PageListFragment.newInstance(pageTypes[position])
        listFragments[pageTypes[position]] = WeakReference(fragment)
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(pageTypes[position].title)
    }

    fun scrollToPage(page: PageModel) {
        val listFragment = listFragments[PageListType.fromPageStatus(page.status)]?.get()
        listFragment?.scrollToPage(page.remoteId)
    }
}
