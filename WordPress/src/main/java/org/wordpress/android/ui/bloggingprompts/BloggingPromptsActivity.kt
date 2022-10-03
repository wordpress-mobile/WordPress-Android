package org.wordpress.android.ui.bloggingprompts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingPromptsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

class BloggingPromptsActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = BloggingPromptsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun start(
            context: Context,
            siteId: Long,
        ) = context.startActivity(buildIntent(context, siteId))

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            siteId: Long,
        ) = Intent(context, BloggingPromptsActivity::class.java).apply {
            putExtra(WordPress.LOCAL_SITE_ID, siteId)
        }
    }
}
