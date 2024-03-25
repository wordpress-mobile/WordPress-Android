package org.wordpress.android.ui.posts.prepublishing.home

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PostPrepublishingHomeFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingActionClickedListener
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingSocialViewModelProvider
import org.wordpress.android.ui.stats.refresh.utils.WrappingLinearLayoutManager
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingHomeFragment : Fragment(R.layout.post_prepublishing_home_fragment) {
    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingHomeViewModel
    private lateinit var jetpackSocialViewModel: EditorJetpackSocialViewModel

    private var actionClickedListener: PrepublishingActionClickedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actionClickedListener = parentFragment as PrepublishingActionClickedListener
    }

    override fun onDetach() {
        super.onDetach()
        actionClickedListener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PostPrepublishingHomeFragmentBinding.bind(view)) {
            setupRecyclerView()
            initViewModel()
            setupJetpackSocialViewModel()
        }
    }

    private fun PostPrepublishingHomeFragmentBinding.setupRecyclerView() {
        val adapter = PrepublishingHomeAdapter(requireActivity())
        // use WrappingLinearLayoutManager to properly handle recycler with wrap_content height
        val layoutManager = WrappingLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )

        adapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    layoutManager.onItemRangeRemoved()
                }
            }
        )

        // since the recycler is anchored at the bottom, this is needed so the animation shrinks the item from the top
        layoutManager.stackFromEnd = true

        actionsRecyclerView.layoutManager = layoutManager
        actionsRecyclerView.adapter = adapter
        actionsRecyclerView.isNestedScrollingEnabled = false
    }

    private fun PostPrepublishingHomeFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(this@PrepublishingHomeFragment, viewModelFactory)
            .get(PrepublishingHomeViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.let { (actionsRecyclerView.adapter as PrepublishingHomeAdapter).update(it) }
        }

        viewModel.onActionClicked.observeEvent(viewLifecycleOwner) { actionType ->
            actionClickedListener?.onActionClicked(actionType)
        }

        viewModel.onSubmitButtonClicked.observeEvent(viewLifecycleOwner) { publishPost ->
            actionClickedListener?.onSubmitButtonClicked(publishPost)
        }

        viewModel.start(getEditPostRepository(), getSite())
    }

    private fun setupJetpackSocialViewModel() {
        jetpackSocialViewModel = (parentFragment as PrepublishingSocialViewModelProvider)
            .getEditorJetpackSocialViewModel()

        merge(
            jetpackSocialViewModel.jetpackSocialUiState,
            jetpackSocialViewModel.jetpackSocialContainerVisibility
        ) { uiState, visibility ->
            Pair(uiState, visibility)
        }.observe(viewLifecycleOwner) { pair ->
            val uiState = pair.first ?: return@observe
            val visibility = pair.second ?: return@observe
            viewModel.updateJetpackSocialState(uiState.takeIf { visibility.showInPrepublishingSheet })
        }
    }

    private fun getSite(): SiteModel {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "EditPostActivityHook shouldn't be null."
        }

        return editPostActivityHook.site
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "This is possibly null because it's " +
                    "called during config changes."
        }

        return editPostActivityHook.editPostRepository
    }

    private fun getEditPostActivityHook(): EditPostSettingsFragment.EditPostActivityHook? {
        val activity = activity ?: return null
        return if (activity is EditPostSettingsFragment.EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    companion object {
        const val TAG = "prepublishing_home_fragment_tag"

        fun newInstance() = PrepublishingHomeFragment()
    }
}
