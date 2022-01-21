package org.wordpress.android.ui.comments.unified

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentsEditActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity

class UnifiedCommentsEditActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(UnifiedCommentsEditActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
        }

        val site = intent.getSerializableExtra(KEY_SITE_MODEL) as SiteModel
        val commentId: Int = intent.getIntExtra(KEY_COMMENT_ID, 0)

        val fm = supportFragmentManager
        var editCommentFragment = fm.findFragmentByTag(
                TAG_UNIFIED_EDIT_COMMENT_FRAGMENT
        ) as? UnifiedCommentsEditFragment

        if (editCommentFragment == null) {
            editCommentFragment = UnifiedCommentsEditFragment.newInstance(site, commentId)
            fm.beginTransaction()
                    .add(R.id.fragment_container, editCommentFragment, TAG_UNIFIED_EDIT_COMMENT_FRAGMENT)
                    .commit()
        }
    }

    companion object {

        @JvmStatic
        fun createIntent(
            context: Context,
            commentId: Int?,
            commentSource: CommentSource,
            siteModel: SiteModel,
        ): Intent =
                Intent(context, UnifiedCommentsEditActivity::class.java).apply {
                    putExtra(KEY_COMMENT_ID, commentId)
                    putExtra(KEY_COMMENT_SOURCE, commentSource)
                    putExtra(KEY_SITE_MODEL, siteModel)
                }

        private const val KEY_COMMENT_ID = "key_comment_id"
        private const val KEY_COMMENT_SOURCE = "key_comment_source"
        private const val KEY_SITE_MODEL = "key_site_model"
        private const val TAG_UNIFIED_EDIT_COMMENT_FRAGMENT = "tag_unified_edit_comment_fragment"
    }
}
