package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class PrepublishingTagsFragment : TagsFragment(), TagsSelectedListener {
    private lateinit var toolbarTitle: TextView
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var editPostRepository: EditPostRepository
    private lateinit var viewModel: PrepublishingTagsViewModel

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

    override fun getTagsFromEditPostRepositoryOrArguments() = viewModel.getPostTags()

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
        initViewModel()

        val closeButton = view.findViewById<ImageView>(R.id.close_button)
        val backButton = view.findViewById<ImageView>(R.id.back_button)
        toolbarTitle = view.findViewById(R.id.toolbar_title)

        closeButton.setOnClickListener { viewModel.onCloseButtonClicked() }
        backButton.setOnClickListener {
            viewModel.onBackButtonClicked()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingTagsViewModel::class.java)

        viewModel.dismissBottomSheet.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onCloseClicked()
            }
        })

        viewModel.navigateToHomeScreen.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                ActivityUtils.hideKeyboard(requireActivity())
                closeListener?.onBackClicked()
            }
        })

        viewModel.updateToolbarTitle.observe(this, Observer { uiString ->
            toolbarTitle.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.start(editPostRepository)
    }

    override fun onTagsSelected(selectedTags: String) {
        viewModel.onTagsSelected(selectedTags)
    }
}
