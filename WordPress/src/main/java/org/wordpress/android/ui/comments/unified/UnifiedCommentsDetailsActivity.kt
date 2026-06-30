package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentsDetailsActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.getSerializableExtraCompat

class UnifiedCommentsDetailsActivity : BaseAppCompatActivity() {
    private var binding: UnifiedCommentsDetailsActivityBinding? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        binding = UnifiedCommentsDetailsActivityBinding.inflate(layoutInflater).apply {
            setContentView(root)
            setupActionBar()
        }

        val site = requireNotNull(intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE))
        val remoteCommentId = intent.getLongExtra(KEY_REMOTE_COMMENT_ID, 0)

        val fm = supportFragmentManager
        if (fm.findFragmentByTag(TAG_UNIFIED_COMMENT_DETAILS_FRAGMENT) == null) {
            val fragment = UnifiedCommentDetailsFragment.newInstance(site, remoteCommentId)
            fm.beginTransaction()
                .add(R.id.fragment_container, fragment, TAG_UNIFIED_COMMENT_DETAILS_FRAGMENT)
                .commit()
        }
    }

    private fun UnifiedCommentsDetailsActivityBinding.setupActionBar() {
        setSupportActionBar(toolbarMain)
        supportActionBar?.let {
            it.setTitle(R.string.comment)
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, site: SiteModel, remoteCommentId: Long): Intent =
            Intent(context, UnifiedCommentsDetailsActivity::class.java).apply {
                putExtra(WordPress.SITE, site)
                putExtra(KEY_REMOTE_COMMENT_ID, remoteCommentId)
            }

        private const val KEY_REMOTE_COMMENT_ID = "key_remote_comment_id"
        private const val TAG_UNIFIED_COMMENT_DETAILS_FRAGMENT = "tag_unified_comment_details_fragment"
    }
}
