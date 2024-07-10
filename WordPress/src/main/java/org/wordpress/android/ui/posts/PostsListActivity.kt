@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PostListActivityBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.bloggingreminders.BloggingReminderUtils.observeBottomSheet
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.adapters.AuthorSelectionAdapter
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetFragment
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetFragment.Companion.newInstance
import org.wordpress.android.ui.posts.prepublishing.home.PublishPost
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingBottomSheetListener
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.extensions.setLiftOnScrollTargetViewIdAndRequestLayout
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.AppReviewManager
import javax.inject.Inject
import android.R as AndroidR

const val EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId"
const val STATE_KEY_PREVIEW_STATE = "stateKeyPreviewState"
const val STATE_KEY_BOTTOMSHEET_POST_ID = "stateKeyBottomSheetPostId"

class PostsListActivity : LocaleAwareActivity(),
    EditPostActivityHook,
    PrepublishingBottomSheetListener,
    BasicDialogPositiveClickInterface,
    BasicDialogNegativeClickInterface,
    BasicDialogOnDismissByOutsideTouchInterface,
    ScrollableViewInitializedListener,
    PostResolutionOverlayListener {
    @Inject
    internal lateinit var siteStore: SiteStore

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    internal lateinit var uiHelpers: UiHelpers

    @Inject
    internal lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper

    @Inject
    internal lateinit var previewStateHelper: PreviewStateHelper

    @Inject
    internal lateinit var progressDialogHelper: ProgressDialogHelper

    @Inject
    internal lateinit var dispatcher: Dispatcher

    @Inject
    internal lateinit var uploadActionUseCase: UploadActionUseCase

    @Inject
    internal lateinit var snackbarSequencer: SnackbarSequencer

    @Inject
    internal lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Inject
    internal lateinit var systemNotificationTracker: SystemNotificationsTracker

    @Inject
    internal lateinit var editPostRepository: EditPostRepository

    @Inject
    internal lateinit var mediaPickerLauncher: MediaPickerLauncher

    @Inject
    internal lateinit var bloggingRemindersViewModel: BloggingRemindersViewModel

    @Inject
    internal lateinit var blazeFeatureUtils: BlazeFeatureUtils

    private lateinit var site: SiteModel
    private lateinit var binding: PostListActivityBinding

    override fun getSite() = site
    override fun getEditPostRepository() = editPostRepository

    private lateinit var viewModel: PostListMainViewModel

    private lateinit var postsPagerAdapter: PostsPagerAdapter
    private lateinit var searchActionButton: MenuItem
    private lateinit var authorFilterMenuItem: MenuItem
    private lateinit var authorFilterSpinner: Spinner

    private var restorePreviousSearch = false

    @Suppress("DEPRECATION")
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
        val site = requireNotNull(intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE))
        if (site.id != this.site.id) {
            finish()
            startActivity(intent)
            return
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        with(PostListActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this

            site = requireNotNull(
                if (savedInstanceState == null) {
                    intent.getSerializableExtraCompat(WordPress.SITE)
                } else {
                    restorePreviousSearch = true
                    savedInstanceState.getSerializableCompat(WordPress.SITE)
                }
            ) { "SiteModel cannot be null, check the PendingIntent starting PostsListActivity" }

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

            val tabIndex = intent.getIntExtra(TAB_INDEX, PostListType.PUBLISHED.ordinal)

            setupActionBar()
            setupContent()
            initViewModel(initPreviewState, currentBottomSheetPostId)
            initSearchFragment()
            initBloggingReminders()
            initTabLayout(tabIndex)
            loadIntentData(intent)
        }
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        title = getString(R.string.my_site_btn_blog_posts)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun PostListActivityBinding.setupContent() {
        // Just a safety measure - there shouldn't by any existing listeners since this method is called just once.
        postPager.clearOnPageChangeListeners()

        // this method call needs to be below `clearOnPageChangeListeners` as it internally adds an OnPageChangeListener
        tabLayout.setupWithViewPager(postPager)
        postPager.addOnPageChangeListener(onPageChangeListener)
        fabButton.setOnClickListener {
            viewModel.fabClicked()
        }

        fabButton.setOnLongClickListener {
            if (fabButton.isHapticFeedbackEnabled) {
                fabButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            Toast.makeText(fabButton.context, R.string.create_post_fab_tooltip, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        fabButton.redirectContextClickToLongPressListener()

        postsPagerAdapter = PostsPagerAdapter(POST_LIST_PAGES, site, supportFragmentManager)
        postPager.adapter = postsPagerAdapter
    }

    private fun PostListActivityBinding.initTabLayout(tabIndex: Int) {
       // Notification opens in Drafts tab
        tabLayout.getTabAt(tabIndex)?.select()
    }

    private fun PostListActivityBinding.initViewModel(
        initPreviewState: PostListRemotePreviewState,
        currentBottomSheetPostId: LocalId
    ) {
        viewModel = ViewModelProvider(this@PostsListActivity, viewModelFactory)[PostListMainViewModel::class.java]
        viewModel.start(site, initPreviewState, currentBottomSheetPostId, editPostRepository)

        viewModel.viewState.observe(this@PostsListActivity) { state ->
            state?.let {
                loadViewState(state)
            }
        }

        viewModel.postListAction.observe(this@PostsListActivity) { postListAction ->
            postListAction?.let { action ->
                handlePostListAction(
                    this@PostsListActivity,
                    action,
                    remotePreviewLogicHelper,
                    previewStateHelper,
                    blazeFeatureUtils
                )
            }
        }
        viewModel.selectTab.observe(this@PostsListActivity) { tabIndex ->
            tabIndex?.let {
                tabLayout.getTabAt(tabIndex)?.select()
            }
        }
        viewModel.scrollToLocalPostId.observe(this@PostsListActivity) { targetLocalPostId ->
            targetLocalPostId?.let {
                postsPagerAdapter.getItemAtPosition(postPager.currentItem)?.scrollToTargetPost(targetLocalPostId)
            }
        }
        viewModel.snackBarMessage.observe(this@PostsListActivity) {
            it?.let { snackBarHolder -> showSnackBar(snackBarHolder) }
        }
        viewModel.toastMessage.observe(this@PostsListActivity) {
            it?.show(this@PostsListActivity)
        }
        viewModel.previewState.observe(this@PostsListActivity) {
            progressDialog = progressDialogHelper.updateProgressDialogState(
                this@PostsListActivity,
                progressDialog,
                it.progressDialogUiState,
                uiHelpers
            )
        }
        setupActions()
        viewModel.openPrepublishingBottomSheet.observeEvent(this@PostsListActivity) {
            val fragment = supportFragmentManager.findFragmentByTag(PrepublishingBottomSheetFragment.TAG)
            if (fragment == null) {
                val prepublishingFragment = newInstance(
                    site = site,
                    isPage = editPostRepository.isPage
                )
                prepublishingFragment.show(supportFragmentManager, PrepublishingBottomSheetFragment.TAG)
            }
        }
    }

    private fun initBloggingReminders() {
        bloggingRemindersViewModel = ViewModelProvider(
            this,
            viewModelFactory
        )[BloggingRemindersViewModel::class.java]

        observeBottomSheet(
            bloggingRemindersViewModel.isBottomSheetShowing,
            this,
            BLOGGING_REMINDERS_FRAGMENT_TAG
        ) {
            if (!this.isFinishing) {
                this.supportFragmentManager
            } else {
                null
            }
        }
    }

    private fun PostListActivityBinding.setupActions() {
        viewModel.dialogAction.observe(this@PostsListActivity) {
            it?.show(this@PostsListActivity, supportFragmentManager, uiHelpers)
        }
        viewModel.conflictResolutionAction.observe(this@PostsListActivity) {
            val fragment = supportFragmentManager.findFragmentByTag(PostResolutionOverlayFragment.TAG)
            if (fragment == null) {
                PostResolutionOverlayFragment
                    .newInstance(it.postModel, it.postResolutionType)
                    .show(supportFragmentManager, PostResolutionOverlayFragment.TAG)
            }
        }
        viewModel.postUploadAction.observe(this@PostsListActivity) {
            it?.let { uploadAction ->
                handleUploadAction(
                    uploadAction,
                    this@PostsListActivity,
                    findViewById(R.id.coordinator),
                    uploadActionUseCase,
                    uploadUtilsWrapper
                ) { isFirstTimePublishing ->
                    changeTabsOnPostUpload()
                    bloggingRemindersViewModel.onPublishingPost(site.id, isFirstTimePublishing)
                    if (isFirstTimePublishing) {
                        AppReviewManager.onPostPublished()
                    }
                }
            }
        }
    }

    private fun PostListActivityBinding.changeTabsOnPostUpload() {
        tabLayout.getTabAt(PostListType.PUBLISHED.ordinal)?.select()
    }

    private fun PostListActivityBinding.loadViewState(state: PostListMainViewState) {
        if (state.isFabVisible) {
            fabButton.show()
        } else {
            fabButton.hide()
        }

        // The author selection is in the toolbar, which doesn't get initialized until
        // after loadViewState is invoked. After the toolbar is initialized, the state is
        // updated and the adapter can be set properly. The visibility of the author filter
        // is handled in the viewModel itself
        if (::authorFilterMenuItem.isInitialized && ::authorFilterSpinner.isInitialized) {
            authorFilterMenuItem.isVisible = state.isAuthorFilterVisible

            val authorSelectionAdapter = authorFilterSpinner.adapter as AuthorSelectionAdapter
            authorSelectionAdapter.updateItems(state.authorFilterItems)

            authorSelectionAdapter.getIndexOfSelection(state.authorFilterSelection)?.let { selectionIndex ->
                authorFilterSpinner.setSelection(selectionIndex)
            }
        }
    }

    private fun showSnackBar(holder: SnackbarMessageHolder) {
        findViewById<View>(R.id.coordinator)?.let { parent ->
            snackbarSequencer.enqueue(
                SnackbarItem(
                    SnackbarItem.Info(
                        view = parent,
                        textRes = holder.message,
                        duration = Snackbar.LENGTH_LONG
                    ),
                    holder.buttonTitle?.let {
                        SnackbarItem.Action(
                            textRes = holder.buttonTitle,
                            clickListener = { holder.buttonAction() }
                        )
                    },
                    dismissCallback = { _, event -> holder.onDismissAction(event) }
                )
            )
        }
    }

    private fun loadIntentData(intent: Intent) {
        if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
            val notificationType = requireNotNull(
                intent.getSerializableExtraCompat<NotificationType>(ARG_NOTIFICATION_TYPE)
            )
            systemNotificationTracker.trackTappedNotification(notificationType)
        }

        val targetPostId = intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, -1)
        if (targetPostId != -1) {
            viewModel.showTargetPost(targetPostId)
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.POSTS)
        if (AppReviewManager.shouldShowInAppReviewsPrompt()) {
            AppReviewManager.launchInAppReviews(this)
        }
    }
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == RequestCodes.EDIT_POST && resultCode == Activity.RESULT_OK -> {
                if (data != null && EditPostActivity.checkToRestart(data)) {
                    ActivityLauncher.editPostOrPageForResult(
                        data, this, site,
                        data.getIntExtra(EditPostActivityConstants.EXTRA_POST_LOCAL_ID, 0)
                    )
                    // a restart will happen so, no need to continue here
                    return
                }

                viewModel.handleEditPostResult(data)
            }

            requestCode == RequestCodes.REMOTE_PREVIEW_POST -> {
                viewModel.handleRemotePreviewClosing()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        } else if (item.itemId == R.id.author_filter_menu_item) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.posts_and_pages_list_menu, menu)
        authorFilterMenuItem = menu.findItem(R.id.author_filter_menu_item)
        searchActionButton = menu.findItem(R.id.toggle_search)

        binding.initSearchView()
        initAuthorFilter(authorFilterMenuItem)
        return true
    }

    @SuppressLint("CommitTransaction")
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

    private fun initAuthorFilter(menuItem: MenuItem) {
        // Get the action view (Spinner) from the menu item
        val actionView = menuItem.actionView
        if (actionView is Spinner) {
            authorFilterSpinner = actionView
            val authorSelectionAdapter = AuthorSelectionAdapter(this@PostsListActivity)
            authorFilterSpinner.adapter = authorSelectionAdapter

            // Set a listener if needed
            authorFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.updateAuthorFilterSelection(id)
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // Do nothing here
                }
            }
            viewModel.refreshUiStateForAuthorFilter()
        }
    }
    private fun PostListActivityBinding.initSearchView() {
        searchActionButton.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                viewModel.onSearchExpanded(restorePreviousSearch)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
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

        viewModel.isSearchExpanded.observe(this@PostsListActivity) { isExpanded ->
            authorFilterMenuItem.isVisible = !isExpanded
            toggleSearch(isExpanded)
        }
    }

    private fun PostListActivityBinding.toggleSearch(isExpanded: Boolean) {
        val tabContainer = findViewById<View>(R.id.tabContainer)
        val searchContainer = findViewById<View>(R.id.search_container)

        if (isExpanded) {
            postPager.visibility = View.GONE
            tabContainer.visibility = View.GONE
            searchContainer.visibility = View.VISIBLE
            if (!searchActionButton.isActionViewExpanded) {
                searchActionButton.expandActionView()
            }
            appbarMain.setLiftOnScrollTargetViewIdAndRequestLayout(R.id.posts_search_recycler_view_id)
        } else {
            postPager.visibility = View.VISIBLE
            tabContainer.visibility = View.VISIBLE
            searchContainer.visibility = View.GONE
            if (searchActionButton.isActionViewExpanded) {
                searchActionButton.collapseActionView()
            }
            appbarMain.getTag(R.id.posts_non_search_recycler_view_id_tag_key)?.let {
                appbarMain.setLiftOnScrollTargetViewIdAndRequestLayout(it as Int)
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

    override fun onSubmitButtonClicked(publishPost: PublishPost) {
        viewModel.onBottomSheetPublishButtonClicked()
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        binding.appbarMain.setLiftOnScrollTargetViewIdAndRequestLayout(containerId)
        binding.appbarMain.setTag(R.id.posts_non_search_recycler_view_id_tag_key, containerId)
    }

    // PostResolutionOverlayListener Callbacks
    override fun onPostResolutionConfirmed(event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent) {
        viewModel.onPostResolutionConfirmed(event)
    }

    companion object {
        private const val BLOGGING_REMINDERS_FRAGMENT_TAG = "blogging_reminders_fragment_tag"
        private const val ACTIONS_SHOWN_BY_DEFAULT = "actions_shown_by_default"
        private const val TAB_INDEX = "tab_index"

        @JvmStatic
        fun buildIntent(context: Context, site: SiteModel): Intent {
            val intent = Intent(context, PostsListActivity::class.java)
            intent.putExtra(WordPress.SITE, site)
            return buildIntent(context, site, PostListType.PUBLISHED, false)
        }

        @JvmStatic
        fun buildIntent(
            context: Context,
            site: SiteModel,
            postListType: PostListType,
            actionsShownByDefault: Boolean,
            notificationType: NotificationType? = null
        ): Intent {
            val intent = Intent(context, PostsListActivity::class.java)
            intent.putExtra(WordPress.SITE, site)
            intent.putExtra(ACTIONS_SHOWN_BY_DEFAULT, actionsShownByDefault)
            intent.putExtra(TAB_INDEX, postListType.ordinal)
            if (notificationType != null) {
                intent.putExtra(ARG_NOTIFICATION_TYPE, notificationType)
            }
            return intent
        }
    }
}
