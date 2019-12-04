package org.wordpress.android.ui.posts

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
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.adapters.AuthorSelectionAdapter
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.redirectContextClickToLongPressListener
import javax.inject.Inject

const val EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId"
const val STATE_KEY_PREVIEW_STATE = "stateKeyPreviewState"

class PostsListActivity : AppCompatActivity(),
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

    private lateinit var site: SiteModel
    private lateinit var viewModel: PostListMainViewModel

    private lateinit var authorSelectionAdapter: AuthorSelectionAdapter
    private lateinit var authorSelection: AppCompatSpinner

    private lateinit var tabLayout: TabLayout
    private lateinit var tabLayoutFadingEdge: View

    private lateinit var postsPagerAdapter: PostsPagerAdapter
    private lateinit var pager: androidx.viewpager.widget.ViewPager
    private lateinit var fab: FloatingActionButton
    private lateinit var searchActionButton: MenuItem
    private lateinit var toggleViewLayoutMenuItem: MenuItem

    private var restorePreviousSearch = false

    private var progressDialog: ProgressDialog? = null

    private var onPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            viewModel.onTabChanged(position)
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
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

        setupActionBar()
        setupContent()
        initViewModel(initPreviewState)
        loadIntentData(intent)
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
        tabLayoutFadingEdge = findViewById(R.id.post_list_tab_layout_fading_edge)

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
        fab.setOnClickListener { viewModel.newPost() }
        fab.setOnLongClickListener {
            if (fab.isHapticFeedbackEnabled) {
                fab.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            Toast.makeText(fab.context, R.string.posts_empty_list_button, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }
        fab.redirectContextClickToLongPressListener()

        postsPagerAdapter = PostsPagerAdapter(POST_LIST_PAGES, site, supportFragmentManager)
        pager.adapter = postsPagerAdapter
    }

    private fun initViewModel(initPreviewState: PostListRemotePreviewState) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PostListMainViewModel::class.java)
        viewModel.start(site, initPreviewState)

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
        val targetPostId = intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, -1)
        if (targetPostId != -1) {
            viewModel.showTargetPost(targetPostId)
        }
    }

    public override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.POSTS)
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
}
