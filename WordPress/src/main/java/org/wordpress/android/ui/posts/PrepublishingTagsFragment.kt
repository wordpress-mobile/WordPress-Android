package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class PrepublishingTagsFragment : TagsFragment(), TagsSelectedListener {
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject lateinit var getPostTagsUseCase: GetPostTagsUseCase
    @Inject lateinit var updatePostTagsUseCase: UpdatePostTagsUseCase
    private lateinit var editPostRepository: EditPostRepository

    override fun getContentLayout() = R.layout.fragment_prepublishing_tags

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
        mTagsSelectedListener = this

        if (activity is EditPostActivityHook) {
            editPostRepository = (activity as EditPostActivityHook).editPostRepository
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun getTagsFromEditPostRepositoryOrArguments() = getPostTagsUseCase.getTags(editPostRepository)

    companion object {
        const val TAG = "prepublishing_tags_fragment_tag"
        @JvmStatic fun newInstance(site: SiteModel): PrepublishingTagsFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
            }
            return PrepublishingTagsFragment().apply { arguments = bundle }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<ImageView>(R.id.close_button)
        val backButton = view.findViewById<ImageView>(R.id.back_button)
        val toolbarTitle = view.findViewById<TextView>(R.id.toolbar_title)

        toolbarTitle.text = context?.getString(R.string.prepublishing_nudges_toolbar_title_tags)

        closeButton.setOnClickListener { closeListener?.onCloseClicked() }
        backButton.setOnClickListener {
            ActivityUtils.hideKeyboard(requireActivity())
            closeListener?.onBackClicked()
        }
    }

    override fun onTagsSelected(selectedTags: String) {
        updatePostTagsUseCase.updateTags(selectedTags, editPostRepository)
    }
}
