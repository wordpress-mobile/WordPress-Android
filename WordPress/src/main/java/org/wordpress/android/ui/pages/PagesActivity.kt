package org.wordpress.android.ui.pages

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.pages_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.posts.GutenbergWarningFragmentDialog.GutenbergWarningDialogLearnMoreLinkClickInterface
import org.wordpress.android.ui.posts.PostUtils

const val EXTRA_PAGE_REMOTE_ID_KEY = "extra_page_remote_id_key"
const val EXTRA_PAGE_PARENT_ID_KEY = "extra_page_parent_id_key"

class PagesActivity : AppCompatActivity(), BasicDialogPositiveClickInterface, BasicDialogNegativeClickInterface,
        GutenbergWarningDialogLearnMoreLinkClickInterface {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pages_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(EXTRA_PAGE_REMOTE_ID_KEY)) {
            val pageId = intent.getLongExtra(EXTRA_PAGE_REMOTE_ID_KEY, -1)
            supportFragmentManager.findFragmentById(id.fragment_container)?.let {
                (it as PagesFragment).onSpecificPageRequested(pageId)
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

    override fun onPositiveClicked(instanceTag: String, extras: Any?) {
        val fragment = supportFragmentManager.findFragmentById(id.fragment_container)
        if (fragment is PagesFragment) {
            if (instanceTag.equals(PostUtils.TAG_GUTENBERG_CONFIRM_DIALOG)) {
                    if (extras != null && extras is String) {
                        fragment.onGutenbergEditOk(extras.toInt())
                    }
            } else {
                fragment.onPageDeleteConfirmed(instanceTag.toLong())
            }
        }
    }

    override fun onNegativeClicked(instanceTag: String, extras: Any?) {
        val fragment = supportFragmentManager.findFragmentById(id.fragment_container)
        if (fragment is PagesFragment) {
            if (instanceTag.equals(PostUtils.TAG_GUTENBERG_CONFIRM_DIALOG)) {
                if (extras != null && extras is String) {
                    fragment.onGutenbergWarningDismiss(extras.toInt())
                }
            }
        }
    }

    override fun onLearnMoreLinkClicked(instanceTag: String) {
        // here launch the web the Gutenberg Learn more
        WPWebViewActivity.openURL(this, getString(R.string.dialog_gutenberg_compatibility_learn_more_url))
    }
}
