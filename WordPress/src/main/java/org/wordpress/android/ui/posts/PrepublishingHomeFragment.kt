package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.post_prepublishing_home_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import javax.inject.Inject

class PrepublishingHomeFragment : Fragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingHomeViewModel

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.post_prepublishing_home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions_recycler_view.layoutManager = LinearLayoutManager(requireActivity())
        actions_recycler_view.adapter = PrepublishingHomeAdapter(requireActivity())

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingHomeViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            (actions_recycler_view.adapter as PrepublishingHomeAdapter).update(uiState)
        })

        viewModel.onActionClicked.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { actionType ->
                actionClickedListener?.onActionClicked(actionType)
            }
        })

        viewModel.onPublishButtonClicked.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { postId ->
                actionClickedListener?.onPublishButtonClicked(postId)
            }
        })

        viewModel.start(getEditPostRepository(), getSite())
    }

    private fun getSite(): SiteModel {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "EditPostActivityHook shouldn't be null."
        }

        return editPostActivityHook.site
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) { "This is possibly null because it's " +
                "called during config changes." }

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

    companion object {
        const val TAG = "prepublishing_home_fragment_tag"
        fun newInstance() = PrepublishingHomeFragment()
    }
}
