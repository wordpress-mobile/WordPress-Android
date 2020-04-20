package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
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
    private lateinit var tagsEditText: EditText
    private lateinit var toolbarTitle: TextView
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: PrepublishingTagsViewModel

    override fun getContentLayout() = R.layout.prepublishing_tags_fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
        mTagsSelectedListener = this
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
        val closeButton = view.findViewById<ImageView>(R.id.close_button)
        val backButton = view.findViewById<ImageView>(R.id.back_button)
        toolbarTitle = view.findViewById(R.id.toolbar_title)
        tagsEditText = view.findViewById(R.id.tags_edit_text)

        closeButton.setOnClickListener { viewModel.onCloseButtonClicked() }
        backButton.setOnClickListener { viewModel.onBackButtonClicked() }
        initViewModel()
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

        viewModel.dismissKeyboard.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                ActivityUtils.hideKeyboardForced(tagsEditText)
            }
        })

        viewModel.navigateToHomeScreen.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onBackClicked()
            }
        })

        viewModel.updateToolbarTitle.observe(this, Observer { uiString ->
            toolbarTitle.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.start(getEditPostRepository())
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook())
        { "This is possibly null because it's called during config changes." }

        return editPostActivityHook.editPostRepository
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null
        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    override fun onTagsSelected(selectedTags: String) {
        viewModel.onTagsSelected(selectedTags)
    }
}
