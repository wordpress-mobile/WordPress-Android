package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel

class PostSettingsTagsFragment : TagsFragment() {
    override fun getContentLayout() = layout.fragment_post_settings_tags

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mTagsSelectedListener = if (context is PostSettingsTagsActivity) {
            context
        } else {
            throw RuntimeException("$context must implement TagsSelectedListener")
        }
    }

    companion object {
        const val TAG = "post_settings_tags_fragment_tag"
        @JvmStatic fun newInstance(site: SiteModel, tags: String?): PostSettingsTagsFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putString(PostSettingsTagsActivity.KEY_TAGS, tags)
            }
            return PostSettingsTagsFragment().apply { arguments = bundle }
        }
    }
}
