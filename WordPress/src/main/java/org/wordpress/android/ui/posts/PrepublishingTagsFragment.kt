package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<ImageView>(R.id.close_button)
        val backButton = view.findViewById<ImageView>(R.id.back_button)

        closeButton.setOnClickListener {}
        backButton.setOnClickListener {}
    }
}
