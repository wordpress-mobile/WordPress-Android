package org.wordpress.android.ui.posts

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.CrashlyticsUtils
import org.wordpress.android.util.LocaleManager
import javax.inject.Inject

const val EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId"

class PostsListActivity : AppCompatActivity(),
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface,
        BasicDialogOnDismissByOutsideTouchInterface {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var site: SiteModel
    private lateinit var viewModel: PostListMainViewModel

    private lateinit var postsPagerAdapter: PostsPagerAdapter
    private lateinit var pager: ViewPager
    private lateinit var fab: FloatingActionButton

    private val currentFragment: PostListFragment?
        get() = postsPagerAdapter.getItemAtPosition(pager.currentItem)

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
        handleIntent(intent)
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
        handleIntent(intent)
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        title = getString(R.string.my_site_btn_blog_posts)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupContent() {
        pager = findViewById(R.id.postPager)
        postsPagerAdapter = PostsPagerAdapter(
                POST_LIST_PAGES, site,
                supportFragmentManager
        )
        pager.adapter = postsPagerAdapter
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.setupWithViewPager(pager)

        // Just a safety measure - there shouldn't by any existing listeners since this method is called just once.
        pager.clearOnPageChangeListeners()

        pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                viewModel.onTabChanged(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        fab = findViewById(R.id.fab_button)
        fab.setOnClickListener { viewModel.newPost() }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PostListMainViewModel::class.java)
        viewModel.start(site)
        viewModel.postListAction.observe(this, Observer { postListAction ->
            postListAction?.let { action ->
                handlePostListAction(this@PostsListActivity, action)
            }
        })
        viewModel.isFabVisible.observe(this, Observer { show ->
            show?.let {
                if (show) {
                    fab.show()
                } else {
                    fab.hide()
                }
            }
        })
    }

    private fun handleIntent(intent: Intent) {
        val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
        if (site.id != this.site.id) {
            // restart the activity when the site has changed
            finish()
            startActivity(intent)
            return
        }
    }

    public override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.POSTS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCodes.EDIT_POST) {
            currentFragment?.handleEditPostResult(resultCode, data) ?: logFragmentNullError()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
    }

    // BasicDialogFragment Callbacks

    override fun onPositiveClicked(instanceTag: String) {
        currentFragment?.onPositiveClickedForBasicDialog(instanceTag) ?: logFragmentNullError()
    }

    override fun onNegativeClicked(instanceTag: String) {
        currentFragment?.onNegativeClickedForBasicDialog(instanceTag) ?: logFragmentNullError()
    }

    override fun onDismissByOutsideTouch(instanceTag: String) {
        currentFragment?.onDismissByOutsideTouchForBasicDialog(instanceTag) ?: logFragmentNullError()
    }

    private fun logFragmentNullError() {
        AppLog.e(AppLog.T.POSTS, "CurrentFragment should never be null.")
        CrashlyticsUtils.log("${PostsListActivity::class.java}: CurrentFragment should never be null.")
    }
}
