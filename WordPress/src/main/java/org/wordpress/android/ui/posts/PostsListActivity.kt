package org.wordpress.android.ui.posts

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatSpinner
import android.support.v7.widget.Toolbar
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.adapters.AuthorSelectionAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

const val EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId"

class PostsListActivity : AppCompatActivity(),
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        BasicDialogOnDismissByOutsideTouchInterface {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers

    private lateinit var site: SiteModel
    private lateinit var viewModel: PostListMainViewModel

    private lateinit var authorSelectionAdapter: AuthorSelectionAdapter
    private lateinit var authorSelection: AppCompatSpinner

    private lateinit var tabLayout: TabLayout
    private lateinit var tabLayoutFadingEdge: View

    private lateinit var postsPagerAdapter: PostsPagerAdapter
    private lateinit var pager: ViewPager
    private lateinit var fab: FloatingActionButton

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
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        setupActionBar()
        setupContent()
        initViewModel()
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
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PostListMainViewModel::class.java)
        viewModel.start(site)

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
                handlePostListAction(this@PostsListActivity, action)
            }
        })
        viewModel.updatePostsPager.observe(this, Observer { authorFilter ->
            authorFilter?.let {
                val currentItem: Int = pager.currentItem
                postsPagerAdapter = PostsPagerAdapter(POST_LIST_PAGES, site, authorFilter, supportFragmentManager)
                pager.adapter = postsPagerAdapter

                pager.removeOnPageChangeListener(onPageChangeListener)
                pager.currentItem = currentItem
                pager.addOnPageChangeListener(onPageChangeListener)
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
                        findViewById(R.id.coordinator)
                )
            }
        })
    }

    private fun showSnackBar(holder: SnackbarMessageHolder) {
        findViewById<View>(R.id.coordinator)?.let { parent ->
            val message = getString(holder.messageRes)
            val duration = Snackbar.LENGTH_LONG
            val snackBar = WPSnackbar.make(parent, message, duration)
            if (holder.buttonTitleRes != null) {
                snackBar.setAction(getString(holder.buttonTitleRes)) {
                    holder.buttonAction()
                }
            }
            snackBar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    holder.onDismissAction()
                    super.onDismissed(transientBottomBar, event)
                }
            })
            snackBar.show()
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
            val toggleViewLayoutMenuItem = it.findItem(R.id.toggle_post_list_item_layout)
            viewModel.viewLayoutTypeMenuUiState.observe(this, Observer { menuUiState ->
                menuUiState?.let {
                    updateMenuIcon(menuUiState.iconRes, toggleViewLayoutMenuItem)
                    updateMenuTitle(menuUiState.title, toggleViewLayoutMenuItem)
                }
            })
        }
        return true
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
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
