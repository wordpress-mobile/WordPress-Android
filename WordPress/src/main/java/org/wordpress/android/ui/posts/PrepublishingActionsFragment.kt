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
import kotlinx.android.synthetic.main.post_prepublishing_actions_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.PrepublishingScreenState.ActionsState
import javax.inject.Inject

class PrepublishingActionsFragment : Fragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingActionsViewModel

    private lateinit var actionClickedListener: PrepublishingActionClickedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actionClickedListener = parentFragment as PrepublishingActionClickedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.post_prepublishing_actions_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions_recycler_view.layoutManager = LinearLayoutManager(requireActivity())
        actions_recycler_view.adapter = PrepublishingActionsAdapter(requireActivity())

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingActionsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            (actions_recycler_view.adapter as PrepublishingActionsAdapter).update(uiState)
        })

        viewModel.onActionClicked.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { actionType ->
                actionClickedListener.onActionClicked(actionType)
            }
        })

        viewModel.start(arguments?.getParcelable(DATA))
    }

    companion object {
        const val TAG = "prepublishing_actions_fragment_tag"
        const val DATA = "prepublishing_actions_data"

        fun newInstance(actionsState: ActionsState? = null): PrepublishingActionsFragment {
            return PrepublishingActionsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DATA, actionsState)
                }
            }
        }
    }
}
