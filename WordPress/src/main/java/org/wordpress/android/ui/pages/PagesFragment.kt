package org.wordpress.android.ui.pages

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.pages_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.PAGE_FROM_PAGES_LIST
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.mlp.ModalLayoutPickerFragment
import org.wordpress.android.ui.mlp.ModalLayoutPickerFragment.Companion.MODAL_LAYOUT_PICKER_TAG
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.PostListAction.PreviewPost
import org.wordpress.android.ui.posts.PreviewStateHelper
import org.wordpress.android.ui.posts.ProgressDialogHelper
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper
import org.wordpress.android.ui.posts.adapters.AuthorSelectionAdapter
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.redirectContextClickToLongPressListener
import org.wordpress.android.util.setLiftOnScrollTargetViewIdAndRequestLayout
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListState.FETCHING
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import org.wordpress.android.viewmodel.pages.PagesViewModel
import org.wordpress.android.widgets.WPSnackbar
import java.lang.ref.WeakReference
import javax.inject.Inject

class PagesFragment : Fragment(), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagesViewModel
    private lateinit var mlpViewModel: ModalLayoutPickerViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var actionMenuItem: MenuItem

    /**
     * PostStore needs to be injected here as otherwise FluxC doesn't accept emitted events.
     */
    @Suppress("unused")
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper
    @Inject lateinit var previewStateHelper: PreviewStateHelper
    @Inject lateinit var progressDialogHelper: ProgressDialogHelper
    @Inject lateinit var uploadActionUseCase: UploadActionUseCase
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    private var progressDialog: ProgressDialog? = null

    private var restorePreviousSearch = false

    private lateinit var authorSelectionAdapter: AuthorSelectionAdapter

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

        val nonNullActivity = requireActivity()
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(nonNullActivity)
        initializeViewModels(nonNullActivity, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.EDIT_POST && resultCode == Activity.RESULT_OK && data != null) {
            if (EditPostActivity.checkToRestart(data)) {
                ActivityLauncher.editPageForResult(
                        data,
                        this@PagesFragment,
                        viewModel.site,
                        data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0),
                        false
                )

                // a restart will happen so, no need to continue here
                return
            }
            // we need to work with local ids, since local drafts don't have remote ids
            val localPageId = data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, -1)
            if (localPageId != -1) {
                viewModel.onPageEditFinished(localPageId, data)
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

        newPageButton.setOnLongClickListener {
            if (newPageButton.isHapticFeedbackEnabled) {
                newPageButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            Toast.makeText(newPageButton.context, R.string.create_page_fab_tooltip, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }
        newPageButton.redirectContextClickToLongPressListener()

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

        authorSelectionAdapter = AuthorSelectionAdapter(activity)
        pages_author_selection.adapter = authorSelectionAdapter
        pages_author_selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {}

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.updateAuthorFilterSelection(id)
            }
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
        viewModel = ViewModelProvider(activity, viewModelFactory).get(PagesViewModel::class.java)
        mlpViewModel = ViewModelProvider(activity, viewModelFactory).get(ModalLayoutPickerViewModel::class.java)

        setupObservers(activity)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(activity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            restorePreviousSearch = true
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        viewModel.authorUIState.observe(activity, Observer { state ->
            state?.let {
                uiHelpers.updateVisibility(pages_author_selection, state.isAuthorFilterVisible)
                uiHelpers.updateVisibility(pages_tab_layout_fading_edge, state.isAuthorFilterVisible)

                val tabLayoutPaddingStart =
                        if (state.isAuthorFilterVisible)
                            resources.getDimensionPixelSize(R.dimen.posts_list_tab_layout_fading_edge_width)
                        else 0
                tabLayout.setPaddingRelative(tabLayoutPaddingStart, 0, 0, 0)

                authorSelectionAdapter.updateItems(state.authorFilterItems)

                authorSelectionAdapter.getIndexOfSelection(state.authorFilterSelection)?.let { selectionIndex ->
                    pages_author_selection.setSelection(selectionIndex)
                }
            }
        })

        viewModel.start(site)
    }

    private fun showToast(toastMessageHolder: ToastMessageHolder) {
        context?.let {
            toastMessageHolder.show(it)
        }
    }

    private fun previewPage(activity: FragmentActivity, post: PostModel) {
        val action = PreviewPost(
                site = viewModel.site,
                post = post,
                triggerPreviewStateUpdate = viewModel::updatePreviewAndDialogState,
                showToast = this::showToast,
                messageMediaUploading = ToastMessageHolder(R.string.editor_toast_uploading_please_wait, Duration.SHORT)
        )

        val helperFunctions = previewStateHelper.getUploadStrategyFunctions(activity, action)
        remotePreviewLogicHelper.runPostPreviewLogic(
                activity = activity,
                site = viewModel.site,
                post = post,
                helperFunctions = helperFunctions
        )
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.listState.observe(viewLifecycleOwner, Observer {
            refreshProgressBars(it)
        })

        viewModel.createNewPage.observe(viewLifecycleOwner, Observer {
            if (mlpViewModel.canShowModalLayoutPicker()) {
                mlpViewModel.createPageFlowTriggered()
            } else {
                createNewPage()
            }
        })

        mlpViewModel.onCreateNewPageRequested.observe(viewLifecycleOwner, Observer { request ->
            createNewPage(request.title, request.content, request.template)
        })

        viewModel.showSnackbarMessage.observe(viewLifecycleOwner, Observer { holder ->
            val parent = activity.findViewById<View>(R.id.coordinatorLayout)
            if (holder != null && parent != null) {
                if (holder.buttonTitle == null) {
                    WPSnackbar.make(
                            parent,
                            uiHelpers.getTextOfUiString(requireContext(), holder.message),
                            Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    val snackbar = WPSnackbar.make(
                            parent,
                            uiHelpers.getTextOfUiString(requireContext(), holder.message),
                            Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction(
                            uiHelpers.getTextOfUiString(requireContext(),
                            holder.buttonTitle)) { holder.buttonAction()
                    }
                    snackbar.show()
                }
            }
        })

        viewModel.editPage.observe(viewLifecycleOwner, Observer { (site, page, loadAutoRevision) ->
            page?.let {
                ActivityLauncher.editPageForResult(this, site, page.id, loadAutoRevision)
            }
        })

        viewModel.previewPage.observe(viewLifecycleOwner, Observer { post ->
            post?.let {
                previewPage(activity, post)
            }
        })

        viewModel.browsePreview.observe(viewLifecycleOwner, Observer { preview ->
            preview?.let {
                ActivityLauncher.previewPostOrPageForResult(activity, viewModel.site, preview.post, preview.previewType)
            }
        })

        viewModel.previewState.observe(viewLifecycleOwner, Observer {
            progressDialog = progressDialogHelper.updateProgressDialogState(
                    activity,
                    progressDialog,
                    it.progressDialogUiState,
                    uiHelpers
            )
        })

        viewModel.setPageParent.observe(viewLifecycleOwner, Observer { page ->
            page?.let { ActivityLauncher.viewPageParentForResult(this, page) }
        })

        viewModel.isNewPageButtonVisible.observe(viewLifecycleOwner, Observer { isVisible ->
            isVisible?.let {
                if (isVisible) {
                    newPageButton.show()
                } else {
                    newPageButton.hide()
                }
            }
        })

        viewModel.scrollToPage.observe(viewLifecycleOwner, Observer { requestedPage ->
            requestedPage?.let { page ->
                val pagerIndex = PagesPagerAdapter.pageTypes.indexOf(PageListType.fromPageStatus(page.status))
                pagesPager.currentItem = pagerIndex
                (pagesPager.adapter as PagesPagerAdapter).scrollToPage(page)
            }
        })

        viewModel.dialogAction.observe(viewLifecycleOwner, Observer {
            it?.show(activity, activity.supportFragmentManager, uiHelpers)
        })

        viewModel.postUploadAction.observe(viewLifecycleOwner, Observer {
            it?.let { (post, site, data) ->
                uploadUtilsWrapper.handleEditPostResultSnackbars(
                        activity,
                        activity.findViewById(R.id.coordinator),
                        data,
                        post,
                        site,
                        uploadActionUseCase.getUploadAction(post),
                        View.OnClickListener {
                            uploadUtilsWrapper.publishPost(
                                    activity,
                                    post,
                                    site
                            )
                        }
                )
            }
        })

        viewModel.publishAction.observe(this, Observer {
            it?.let {
                uploadUtilsWrapper.publishPost(activity, it.post, it.site)
            }
        })

        viewModel.uploadFinishedAction.observe(viewLifecycleOwner, Observer {
            it?.let { (page, isError) ->
                uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                        activity,
                        activity.findViewById(R.id.coordinator),
                        isError,
                        page.post,
                        null,
                        page.site
                )
            }
        })

        mlpViewModel.isModalLayoutPickerShowing.observe(viewLifecycleOwner, Observer { event: Event<Boolean> ->
            event.applyIfNotHandled {
                val fm = activity.supportFragmentManager
                var mlpFragment = fm.findFragmentByTag(MODAL_LAYOUT_PICKER_TAG) as ModalLayoutPickerFragment?
                if (this && mlpFragment == null) {
                    mlpFragment = ModalLayoutPickerFragment()
                    mlpFragment.show(fm, MODAL_LAYOUT_PICKER_TAG)
                } else if (!this && mlpFragment != null) {
                    mlpFragment.dismiss()
                }
            }
        })
    }

    /**
     * Triggers new page creation
     * @param title the page title
     * @param content the page content
     * @param template the selected layout template
     */
    private fun createNewPage(title: String = "", content: String = "", template: String? = null) {
        ActivityLauncher.addNewPageForResult(
                this, viewModel.site, title, content, template,
                PAGE_FROM_PAGES_LIST
        )
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
    }

    private fun hideSearchList(myActionMenuItem: MenuItem) {
        pagesPager.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        tabContainer.visibility = View.VISIBLE
        searchFrame.visibility = View.GONE
        if (myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.collapseActionView()
        }
        appbar_main.getTag(R.id.pages_non_search_recycler_view_id_tag_key)?.let {
            appbar_main.setLiftOnScrollTargetViewIdAndRequestLayout(it as Int)
        }
    }

    private fun showSearchList(myActionMenuItem: MenuItem) {
        pagesPager.visibility = View.GONE
        tabLayout.visibility = View.GONE
        tabContainer.visibility = View.GONE
        searchFrame.visibility = View.VISIBLE
        if (!myActionMenuItem.isActionViewExpanded) {
            myActionMenuItem.expandActionView()
        }
        appbar_main.setLiftOnScrollTargetViewIdAndRequestLayout(R.id.pages_search_recycler_view_id)
    }

    fun onPositiveClickedForBasicDialog(instanceTag: String) {
        viewModel.onPositiveClickedForBasicDialog(instanceTag)
    }

    fun onNegativeClickedForBasicDialog(instanceTag: String) {
        viewModel.onNegativeClickedForBasicDialog(instanceTag)
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        appbar_main.setLiftOnScrollTargetViewIdAndRequestLayout(containerId)
        appbar_main.setTag(R.id.pages_non_search_recycler_view_id_tag_key, containerId)
    }
}

class PagesPagerAdapter(val context: Context, val fm: FragmentManager) : FragmentPagerAdapter(
        fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
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
        listFragment?.scrollToPage(page.pageId)
    }
}
