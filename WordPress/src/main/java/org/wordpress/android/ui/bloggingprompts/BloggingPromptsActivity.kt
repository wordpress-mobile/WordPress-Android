package org.wordpress.android.ui.bloggingprompts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingPromptsActivityBinding
import org.wordpress.android.fluxc.model.SiteModel

@AndroidEntryPoint
class BloggingPromptsActivity : AppCompatActivity() {
    private lateinit var site: SiteModel
    private val viewModel: BloggingPromptsParentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = BloggingPromptsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        site = checkNotNull((intent.getSerializableExtra(WordPress.SITE) as? SiteModel)) {
            "${WordPress.SITE} argument cannot be null, when launching ${BloggingPromptsActivity::class.simpleName}"
        }
        viewModel.start(site)
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
