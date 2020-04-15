package org.wordpress.android.ui.posts

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
import org.wordpress.android.ui.posts.PrepublishingActionState.HomeState
import javax.inject.Inject

class PrepublishingActionsFragment : Fragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingActionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
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
        viewModel = ViewModelProviders.of(this.requireParentFragment(), viewModelFactory)
                .get(PrepublishingActionsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            (actions_recycler_view.adapter as PrepublishingActionsAdapter).update(uiState)
        })

        viewModel.start(arguments?.getParcelable(DATA))
    }

    companion object {
        const val TAG = "prepublishing_actions_fragment_tag"
        const val DATA = "prepublishing_actions_data"

        fun newInstance(homeState: HomeState? = null): PrepublishingActionsFragment {
            return PrepublishingActionsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DATA, homeState)
                }
            }
        }
    }
}
