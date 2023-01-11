package org.wordpress.android.ui.comments.unified

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_FILTER_CHANGED
import org.wordpress.android.databinding.UnifiedCommentActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.comments.unified.CommentFilter.ALL
import org.wordpress.android.ui.comments.unified.CommentFilter.APPROVED
import org.wordpress.android.ui.comments.unified.CommentFilter.PENDING
import org.wordpress.android.ui.comments.unified.CommentFilter.SPAM
import org.wordpress.android.ui.comments.unified.CommentFilter.TRASHED
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class UnifiedCommentsActivity : LocaleAwareActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository
    private lateinit var viewModel: UnifiedCommentActivityViewModel

    private val commentListFilters = listOf(ALL, PENDING, UNREPLIED, APPROVED, SPAM, TRASHED)

    private var disabledTabsOpacity: Float = 0F

    private lateinit var pagerAdapter: UnifiedCommentListPagerAdapter

    private var binding: UnifiedCommentActivityBinding? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        viewModel = ViewModelProvider(this, viewModelFactory).get(UnifiedCommentActivityViewModel::class.java)
        val disabledAlpha = TypedValue()
        resources.getValue(dimen.material_emphasis_disabled, disabledAlpha, true)
        disabledTabsOpacity = disabledAlpha.float

        if (selectedSiteRepository.getSelectedSite() == null) {
            ToastUtils.showToast(this@UnifiedCommentsActivity, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            finish()
        }

        binding = UnifiedCommentActivityBinding.inflate(layoutInflater).apply {
            setContentView(root)
            setupActionBar()
            setupContent()
            setupObservers()
        }
    }

    // for some reason lint is not happy about VIEW_PAGER_OFFSCREEN_PAGE_LIMIT, even through it's a valid constant
    @SuppressLint("WrongConstant")
    private fun UnifiedCommentActivityBinding.setupContent() {
        viewPager.offscreenPageLimit = VIEW_PAGER_OFFSCREEN_PAGE_LIMIT

        pagerAdapter = UnifiedCommentListPagerAdapter(commentListFilters, this@UnifiedCommentsActivity)
        viewPager.adapter = pagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val properties: MutableMap<String, String?> = HashMap()
                properties["selected_filter"] = getString(commentListFilters[position].toTrackingLabelResId())
                AnalyticsTracker.track(COMMENT_FILTER_CHANGED, properties)
                super.onPageSelected(position)
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setText(commentListFilters[position].labelResId)
        }.attach()
    }

    private fun UnifiedCommentActivityBinding.setupActionBar() {
        setSupportActionBar(toolbarMain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun UnifiedCommentActivityBinding.setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                viewPager.isUserInputEnabled = uiState.isTabBarEnabled
                for (i in 0 until tabLayout.tabCount) {
                    tabLayout.getTabAt(i)?.view?.isEnabled = uiState.isTabBarEnabled
                    tabLayout.getTabAt(i)?.view?.isClickable = uiState.isTabBarEnabled
                    if (uiState.isTabBarEnabled) {
                        tabLayout.getTabAt(i)?.view?.alpha = 1F
                    } else {
                        tabLayout.getTabAt(i)?.view?.alpha = disabledTabsOpacity
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val VIEW_PAGER_OFFSCREEN_PAGE_LIMIT = 1
    }
}
