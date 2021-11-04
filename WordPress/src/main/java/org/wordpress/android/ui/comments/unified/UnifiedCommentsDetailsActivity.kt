package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentsDetailsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class UnifiedCommentsDetailsActivity : LocaleAwareActivity() {
    private var binding: UnifiedCommentsDetailsActivityBinding? = null
    private lateinit var pagerAdapter: UnifiedCommentsDetailPagerAdapter

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        binding = UnifiedCommentsDetailsActivityBinding.inflate(layoutInflater).apply {
            setContentView(root)
            setupActionBar()
            setupContent()
        }
    }

    private fun UnifiedCommentsDetailsActivityBinding.setupContent() {
        pagerAdapter = UnifiedCommentsDetailPagerAdapter(this@UnifiedCommentsDetailsActivity)
        viewPager.adapter = pagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            // TODO track subsequent comment views
        })
    }

    private fun UnifiedCommentsDetailsActivityBinding.setupActionBar() {
        setSupportActionBar(toolbarMain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }
}
