package org.wordpress.android.ui.posts

import android.R.color
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.Orientation
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.main_activity.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PrepublishingBottomSheetFragment.Companion.newInstance
import org.wordpress.android.ui.posts.adapters.AuthorSelectionAdapter
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetListener
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.RtlUtils
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.config.WPStoriesFeatureConfig
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.redirectContextClickToLongPressListener
import org.wordpress.android.viewmodel.posts.PostListCreateMenuViewModel
import org.wordpress.android.widgets.WPDialogSnackbar
import javax.inject.Inject

const val EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId"
const val STATE_KEY_PREVIEW_STATE = "stateKeyPreviewState"
const val STATE_KEY_BOTTOMSHEET_POST_ID = "stateKeyBottomSheetPostId"

class PostsListActivity : LocaleAwareActivity(),
        EditPostActivityHook,
        PrepublishingBottomSheetListener,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        BasicDialogOnDismissByOutsideTouchInterface {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    @Inject internal lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper
    @Inject internal lateinit var previewStateHelper: PreviewStateHelper
    @Inject internal lateinit var progressDialogHelper: ProgressDialogHelper
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var uploadActionUseCase: UploadActionUseCase
    @Inject internal lateinit var snackbarSequencer: SnackbarSequencer
    @Inject internal lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject internal lateinit var quickStartStore: QuickStartStore
    @Inject internal lateinit var systemNotificationTracker: SystemNotificationsTracker
    @Inject internal lateinit var editPostRepository: EditPostRepository
    @Inject internal lateinit var wpStoriesFeatureConfig: WPStoriesFeatureConfig

    private lateinit var site: SiteModel

    override fun getSite() = site
    override fun getEditPostRepository() = editPostRepository

    private lateinit var viewModel: PostListMainViewModel
    private lateinit var postListCreateMenuViewModel: PostListCreateMenuViewModel

    private lateinit var authorSelectionAdapter: AuthorSelectionAdapter
    private lateinit var authorSelection: AppCompatSpinner

    private lateinit var tabLayout: TabLayout
    private lateinit var tabLayoutFadingEdge: View

    private lateinit var postsPagerAdapter: PostsPagerAdapter
    private lateinit var pager: androidx.viewpager.widget.ViewPager
    private lateinit var fab: FloatingActionButton
    private lateinit var searchActionButton: MenuItem
    private lateinit var toggleViewLayoutMenuItem: MenuItem

    private var quickStartEvent: QuickStartEvent? = null

    private var restorePreviousSearch = false

    private var progressDialog: ProgressDialog? = null

    private var onPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            viewModel.onTabChanged(position)
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.hasExtra(WordPress.SITE)) {
            AppLog.e(AppLog.T.POSTS, "PostListActivity started without a site.")
            finish()
            return
        }
        restartWhenSiteHasChanged(intent)
        loadIntentData(intent)
    }

    private fun restartWhenSiteHasChanged(intent: Intent) {
        val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
        if (site.id != this.site.id) {
            finish()
            startActivity(intent)
            return
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.post_list_activity)

        site = if (savedInstanceState == null) {
            intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            restorePreviousSearch = true
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        val initPreviewState = if (savedInstanceState == null) {
            PostListRemotePreviewState.NONE
        } else {
            PostListRemotePreviewState.fromInt(savedInstanceState.getInt(STATE_KEY_PREVIEW_STATE, 0))
        }

        val currentBottomSheetPostId = if (savedInstanceState == null) {
            LocalId(0)
        } else {
            LocalId(savedInstanceState.getInt(STATE_KEY_BOTTOMSHEET_POST_ID, 0))
        }

        setupActionBar()
        setupContent()
        initViewModel(initPreviewState, currentBottomSheetPostId)
        initCreateMenuViewModel()
        loadIntentData(intent)

        quickStartEvent = savedInstanceState?.getParcelable(QuickStartEvent.KEY)
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        title = getString(R.string.my_site_btn_blog_posts)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupContent() {
        authorSelection = findViewById(R.id.post_list_author_selection)

        val elevationOverlayProvider = ElevationOverlayProvider(this)
        val appbarElevation = resources.getDimension(R.dimen.appbar_elevation)
        val appBarColor = elevationOverlayProvider.compositeOverlayIfNeeded(
                this.getColorFromAttribute(R.attr.wpColorAppBar),
                appbarElevation
        )

        val fadingEdgeDrawable = GradientDrawable(
                if (RtlUtils.isRtl(this)) {
                    Orientation.LEFT_RIGHT
                } else {
                    Orientation.RIGHT_LEFT
                },
                intArrayOf(ContextCompat.getColor(this, color.transparent), appBarColor)
        )

        tabLayoutFadingEdge = findViewById(R.id.post_list_tab_layout_fading_edge)
        tabLayoutFadingEdge.background = fadingEdgeDrawable

        authorSelectionAdapter = AuthorSelectionAdapter(this)
        authorSelection.adapter = authorSelectionAdapter

        authorSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {}

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.updateAuthorFilterSelection(id)
            }
        }

        pager = findViewById(R.id.postPager)

        // Just a safety measure - there shouldn't by any existing listeners since this method is called just once.
        pager.clearOnPageChangeListeners()

        tabLayout = findViewById(R.id.tabLayout)
        // this method call needs to be below `clearOnPageChangeListeners` as it internally adds an OnPageChangeListener
        tabLayout.setupWithViewPager(pager)
        pager.addOnPageChangeListener(onPageChangeListener)
        fab = findViewById(R.id.fab_button)
        fab.setOnClickListener {
            if (wpStoriesFeatureConfig.isEnabled()) {
                postListCreateMenuViewModel.onFabClicked()
            } else {
                viewModel.newPost()
            }
        }

        fab.setOnLongClickListener {
            if (wpStoriesFeatureConfig.isEnabled()) {
                postListCreateMenuViewModel.onFabLongPressed()
            } else {
                if (fab.isHapticFeedbackEnabled) {
                    fab.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                Toast.makeText(fab.context, R.string.posts_empty_list_button, Toast.LENGTH_SHORT).show()
            }
            return@setOnLongClickListener true
        }
        fab.redirectContextClickToLongPressListener()

        fab_tooltip.setOnClickListener {
            postListCreateMenuViewModel.onTooltipTapped()
        }

        postsPagerAdapter = PostsPagerAdapter(POST_LIST_PAGES, site, supportFragmentManager)
        pager.adapter = postsPagerAdapter
    }

    private fun initCreateMenuViewModel() {
        postListCreateMenuViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PostListCreateMenuViewModel::class.java)

        postListCreateMenuViewModel.isBottomSheetShowing.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { isBottomSheetShowing ->
                var postListCreateMenuFragment = supportFragmentManager.findFragmentByTag(PostListCreateMenuFragment.TAG)
                if (postListCreateMenuFragment == null) {
                    if (isBottomSheetShowing) {
                        postListCreateMenuFragment = PostListCreateMenuFragment.newInstance()
                        postListCreateMenuFragment.show(supportFragmentManager, PostListCreateMenuFragment.TAG)
                    }
                } else {
                    if (!isBottomSheetShowing) {
                        postListCreateMenuFragment as PostListCreateMenuFragment
                        postListCreateMenuFragment.dismiss()
                    }
                }
            }
        })

        postListCreateMenuViewModel.fabUiState.observe(this, Observer { fabUiState ->
            val message = resources.getString(fabUiState.CreateContentMessageId)

            if (fabUiState.isFabTooltipVisible) {
                fab_tooltip.setMessage(message)
                fab_tooltip.show()
            } else {
                fab_tooltip.hide()
            }

            fab.contentDescription = message
        })

        postListCreateMenuViewModel.createAction.observe(this, Observer { createAction ->
            when (createAction) {
                CREATE_NEW_POST -> viewModel.newPost()
                CREATE_NEW_STORY -> viewModel.newStoryPost()
            }
        })
    }

    private fun initViewModel(initPreviewState: PostListRemotePreviewState, currentBottomSheetPostId: LocalId) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PostListMainViewModel::class.java)
        viewModel.start(site, initPreviewState, currentBottomSheetPostId, editPostRepository, this)

        viewModel.viewState.observe(this, Observer { state ->
            state?.let {
                if (state.isFabVisible) {
                    fab.show()
                } else {
                    fab.hide()
                }

                val authorSelectionVisibility = if (state.isAuthorFilterVisible) View.VISIBLE else View.GONE
                authorSelection.visibility = authorSelectionVisibility
                tabLayoutFadingEdge.visibility = authorSelectionVisibility

                val tabLayoutPaddingStart =
                        if (state.isAuthorFilterVisible)
                            resources.getDimensionPixelSize(R.dimen.posts_list_tab_layout_fading_edge_width)
                        else 0
                tabLayout.setPaddingRelative(tabLayoutPaddingStart, 0, 0, 0)

                authorSelectionAdapter.updateItems(state.authorFilterItems)

                authorSelectionAdapter.getIndexOfSelection(state.authorFilterSelection)?.let { selectionIndex ->
                    authorSelection.setSelection(selectionIndex)
                }
            }
        })

        viewModel.postListAction.observe(this, Observer { postListAction ->
            postListAction?.let { action ->
                QuickStartUtils.completeTaskAndRemindNextOne(
                        quickStartStore,
                        QuickStartTask.PUBLISH_POST,
                        dispatcher,
                        site,
                        quickStartEvent,
                        this@PostsListActivity
                )

                handlePostListAction(
                        this@PostsListActivity,
                        action,
                        remotePreviewLogicHelper,
                        previewStateHelper
                )
            }
        })
        viewModel.selectTab.observe(this, Observer { tabIndex ->
            tabIndex?.let {
                tabLayout.getTabAt(tabIndex)?.select()
            }
        })
        viewModel.scrollToLocalPostId.observe(this, Observer { targetLocalPostId ->
            targetLocalPostId?.let {
                postsPagerAdapter.getItemAtPosition(pager.currentItem)?.scrollToTargetPost(targetLocalPostId)
            }
        })
        viewModel.snackBarMessage.observe(this, Observer {
            it?.let { snackBarHolder -> showSnackBar(snackBarHolder) }
        })
        viewModel.previewState.observe(this, Observer {
            progressDialog = progressDialogHelper.updateProgressDialogState(
                    this,
                    progressDialog,
                    it.progressDialogUiState,
                    uiHelpers)
        })
        viewModel.dialogAction.observe(this, Observer {
            it?.show(this, supportFragmentManager, uiHelpers)
        })
        viewModel.toastMessage.observe(this, Observer {
            it?.show(this)
        })
        viewModel.postUploadAction.observe(this, Observer {
            it?.let { uploadAction ->
                handleUploadAction(
                        uploadAction,
                        this@PostsListActivity,
                        findViewById(R.id.coordinator),
                        uploadActionUseCase,
                        uploadUtilsWrapper
                )
            }
        })
        viewModel.openPrepublishingBottomSheet.observe(this, Observer { event ->
            event.applyIfNotHandled {
                val fragment = supportFragmentManager.findFragmentByTag(PrepublishingBottomSheetFragment.TAG)
                if (fragment == null) {
                    val prepublishingFragment = newInstance(
                            site = site,
                            isPage = editPostRepository.isPage,
                            isStoryPost = false
                    )
                    prepublishingFragment.show(supportFragmentManager, PrepublishingBottomSheetFragment.TAG)
                }
            }
        })
    }

    private fun showSnackBar(holder: SnackbarMessageHolder) {
        findViewById<View>(R.id.coordinator)?.let { parent ->
            snackbarSequencer.enqueue(
                    SnackbarItem(
                            SnackbarItem.Info(
                                view = parent,
                                textRes = UiStringRes(holder.messageRes),
                                duration = Snackbar.LENGTH_LONG
                            ),
                            holder.buttonTitleRes?.let {
                                SnackbarItem.Action(
                                    textRes = UiStringRes(holder.buttonTitleRes),
                                    clickListener = OnClickListener { holder.buttonAction() }
                                )
                            },
                            dismissCallback = { _, _ -> holder.onDismissAction() }
                    )
            )
        }
    }

    private fun loadIntentData(intent: Intent) {
        if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
            val notificationType: NotificationType =
                    intent.getSerializableExtra(ARG_NOTIFICATION_TYPE) as NotificationType
            systemNotificationTracker.trackTappedNotification(notificationType)
        }

        val targetPostId = intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, -1)
        if (targetPostId != -1) {
            viewModel.showTargetPost(targetPostId)
        }
    }

    public override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.POSTS)
        postListCreateMenuViewModel.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCodes.EDIT_POST && resultCode == Activity.RESULT_OK) {
            if (data != null && EditPostActivity.checkToRestart(data)) {
                ActivityLauncher.editPostOrPageForResult(
                        data, this, site,
                        data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0)
                )

                // a restart will happen so, no need to continue here
                return
            }

            viewModel.handleEditPostResult(data)
        } else if (requestCode == RequestCodes.REMOTE_PREVIEW_POST) {
            viewModel.handleRemotePreviewClosing()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.toggle_post_list_item_layout) {
            viewModel.toggleViewLayout()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.let {
            menuInflater.inflate(R.menu.posts_list_toggle_view_layout, it)
            toggleViewLayoutMenuItem = it.findItem(R.id.toggle_post_list_item_layout)
            viewModel.viewLayoutTypeMenuUiState.observe(this, Observer { menuUiState ->
                menuUiState?.let {
                    updateMenuIcon(menuUiState.iconRes, toggleViewLayoutMenuItem)
                    updateMenuTitle(menuUiState.title, toggleViewLayoutMenuItem)
                }
            })

            searchActionButton = it.findItem(R.id.toggle_post_search)

            viewModel.isSearchAvailable.observe(this, Observer { isAvailable ->
                if (isAvailable) {
                    initSearchFragment()
                    initSearchView()
                    searchActionButton.isVisible = true
                } else {
                    searchActionButton.isVisible = false
                }
            })
        }
        return true
    }

    private fun initSearchFragment() {
        val searchFragmentTag = "search_fragment"

        var searchFragment = supportFragmentManager.findFragmentByTag(searchFragmentTag)

        if (searchFragment == null) {
            searchFragment = PostListFragment.newInstance(site, SEARCH)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.search_container, searchFragment, searchFragmentTag)
                    .commit()
        }
    }

    private fun initSearchView() {
        searchActionButton.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.onSearchExpanded(restorePreviousSearch)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.onSearchCollapsed()
                return true
            }
        })

        val searchView = searchActionButton.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.onSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (restorePreviousSearch) {
                    restorePreviousSearch = false
                    searchView.setQuery(viewModel.searchQuery.value, false)
                } else {
                    viewModel.onSearch(newText)
                }
                return true
            }
        })

        viewModel.isSearchExpanded.observe(this, Observer { isExpanded ->
            toggleViewLayoutMenuItem.isVisible = !isExpanded
            toggleSearch(isExpanded)
        })
    }

    private fun toggleSearch(isExpanded: Boolean) {
        val tabContainer = findViewById<View>(R.id.tabContainer)
        val searchContainer = findViewById<View>(R.id.search_container)

        if (isExpanded) {
            pager.visibility = View.GONE
            tabContainer.visibility = View.GONE
            searchContainer.visibility = View.VISIBLE
            if (!searchActionButton.isActionViewExpanded) {
                searchActionButton.expandActionView()
            }
        } else {
            pager.visibility = View.VISIBLE
            tabContainer.visibility = View.VISIBLE
            searchContainer.visibility = View.GONE
            if (searchActionButton.isActionViewExpanded) {
                searchActionButton.collapseActionView()
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
        viewModel.previewState.value?.let {
            outState.putInt(STATE_KEY_PREVIEW_STATE, it.value)
        }
        viewModel.currentBottomSheetPostId?.let {
            outState.putInt(STATE_KEY_BOTTOMSHEET_POST_ID, it.value)
        }
    }

    // BasicDialogFragment Callbacks

    override fun onPositiveClicked(instanceTag: String) {
        viewModel.onPositiveClickedForBasicDialog(instanceTag)
    }

    override fun onNegativeClicked(instanceTag: String) {
        viewModel.onNegativeClickedForBasicDialog(instanceTag)
    }

    override fun onDismissByOutsideTouch(instanceTag: String) {
        viewModel.onDismissByOutsideTouchForBasicDialog(instanceTag)
    }

    // Menu PostListViewLayoutType handling

    private fun updateMenuIcon(@DrawableRes iconRes: Int, menuItem: MenuItem) {
        getDrawable(iconRes)?.let { drawable ->
            menuItem.setIcon(drawable)
        }
    }

    private fun updateMenuTitle(title: UiString, menuItem: MenuItem): MenuItem? {
        return menuItem.setTitle(uiHelpers.getTextOfUiString(this@PostsListActivity, title))
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onEvent(event: QuickStartEvent) {
        val view = findViewById<View>(R.id.coordinator)

        if (view == null) {
            return
        }

        EventBus.getDefault().removeStickyEvent(event)
        quickStartEvent = event

        if (quickStartEvent?.task == QuickStartTask.PUBLISH_POST) {
            view.post {
                val title = QuickStartUtils.stylizeQuickStartPrompt(
                        this,
                        R.string.quick_start_dialog_create_new_post_message_short_posts,
                        R.drawable.ic_create_white_24dp
                )

                WPDialogSnackbar.make(
                        view, title,
                        resources.getInteger(R.integer.quick_start_snackbar_duration_ms)
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onSubmitButtonClicked(publishPost: PublishPost) {
        viewModel.onBottomSheetPublishButtonClicked()
    }
}
