package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel

class PrepublishingTagsFragment : TagsFragment() {
    override fun getContentLayout() = R.layout.fragment_prepublishing_tags

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mTagsSelectedListener = parentFragment as TagsSelectedListener
    }

    companion object {
        const val TAG = "prepublishing_tags_fragment_tag"
        @JvmStatic fun newInstance(site: SiteModel, tags: String?): PrepublishingTagsFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putString(PostSettingsTagsActivity.KEY_TAGS, tags)
            }
            return PrepublishingTagsFragment().apply { arguments = bundle }
        }
    }
}
