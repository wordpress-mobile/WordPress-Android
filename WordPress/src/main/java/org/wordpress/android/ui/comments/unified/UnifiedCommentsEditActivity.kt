package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentsEditActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

class UnifiedCommentsEditActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(UnifiedCommentsEditActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
        }

        val site = intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE)
        val commentIdentifier =
            requireNotNull(intent.getParcelableExtraCompat<CommentIdentifier>(KEY_COMMENT_IDENTIFIER))

        val fm = supportFragmentManager
        val editCommentFragment = fm.findFragmentByTag(
            TAG_UNIFIED_EDIT_COMMENT_FRAGMENT
        ) as? UnifiedCommentsEditFragment

        if (editCommentFragment == null) {
            site?.let {
                val fragment = UnifiedCommentsEditFragment.newInstance(it, commentIdentifier)
                fm.beginTransaction()
                    .add(R.id.fragment_container, fragment, TAG_UNIFIED_EDIT_COMMENT_FRAGMENT)
                    .commit()
            }
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(
            context: Context,
            commentIdentifier: CommentIdentifier,
            siteModel: SiteModel
        ): Intent =
            Intent(context, UnifiedCommentsEditActivity::class.java).apply {
                putExtra(KEY_COMMENT_IDENTIFIER, commentIdentifier)
                putExtra(WordPress.SITE, siteModel)
            }

        private const val KEY_COMMENT_IDENTIFIER = "key_comment_identifier"
        private const val TAG_UNIFIED_EDIT_COMMENT_FRAGMENT = "tag_unified_edit_comment_fragment"
    }
}
