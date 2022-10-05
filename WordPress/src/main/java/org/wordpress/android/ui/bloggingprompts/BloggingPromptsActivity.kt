package org.wordpress.android.ui.bloggingprompts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingPromptsListActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity

class BloggingPromptsActivity : LocaleAwareActivity() {
    private lateinit var site: SiteModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(BloggingPromptsListActivityBinding.inflate(layoutInflater).root)

        site = if (savedInstanceState == null) {
            checkNotNull(intent.getSerializableExtra(WordPress.SITE) as? SiteModel) {
                "${WordPress.SITE} argument cannot be null, check the PendingIntent starting ${BloggingPromptsActivity::class.simpleName}"
            }
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
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
        @JvmStatic
        @JvmOverloads
        fun start(
            context: Context,
            site: SiteModel,
        ) = context.startActivity(buildIntent(context, site))

        @JvmStatic
        @JvmOverloads
        fun buildIntent(
            context: Context,
            site: SiteModel,
        ) = Intent(context, BloggingPromptsActivity::class.java).apply {
            putExtra(WordPress.SITE, site)
        }
    }
}
