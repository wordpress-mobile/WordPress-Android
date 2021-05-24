package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.CommentListFragmentBinding
import org.wordpress.android.ui.comments.CommentListItemDecoration
import javax.inject.Inject

class UnifiedCommentListFragment : Fragment(R.layout.comment_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: UnifiedCommentListViewModel

    private var binding: CommentListFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(UnifiedCommentListViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = CommentListFragmentBinding.bind(view).apply {
            setupContentViews()
            setupObservers()
        }
    }

    private fun CommentListFragmentBinding.setupContentViews() {
        val layoutManager = LinearLayoutManager(context)
        commentsRecyclerView.layoutManager = layoutManager
        commentsRecyclerView.addItemDecoration(CommentListItemDecoration(commentsRecyclerView.context))
        commentsRecyclerView.adapter = UnifiedCommentListAdapter(requireContext())
    }

    private fun CommentListFragmentBinding.setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.commentListItems.collectLatest { pagingData ->
                commentsRecyclerView.adapter?.let { it -> (it as UnifiedCommentListAdapter).submitData(pagingData) }
            }
        }
    }

    companion object {
        fun newInstance(): UnifiedCommentListFragment {
            val args = Bundle()
            val fragment = UnifiedCommentListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
