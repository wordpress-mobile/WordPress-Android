package org.wordpress.android.ui.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SupportFormActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.support.ZendeskExtraTags
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.util.SiteUtils

class SupportFormActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SupportFormActivityBinding.inflate(layoutInflater).root)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val ORIGIN_KEY = "ORIGIN_KEY"
        const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"

        @JvmStatic
        fun createIntent(
            context: Context,
            origin: Origin?,
            selectedSite: SiteModel?,
            extraSupportTags: List<String>?
        ) = Intent(context, SupportFormActivity::class.java).apply {
            putExtra(ORIGIN_KEY, origin)

            selectedSite?.let {
                putExtra(WordPress.SITE, it)
            }

            // construct a mutable list to add the related and extra tags
            val tagsList = arrayListOf<String>().apply {
                // add the provided list of tags if any
                extraSupportTags?.let { addAll(it) }

                // Append the "mobile_gutenberg_is_default" tag if gutenberg is set to default for new posts
                if (SiteUtils.isBlockEditorDefaultForNewPost(selectedSite)) {
                    add(ZendeskExtraTags.gutenbergIsDefault)
                }
            }

            if (tagsList.isNotEmpty()) {
                putStringArrayListExtra(EXTRA_TAGS_KEY, tagsList)
            }
        }
    }
}
