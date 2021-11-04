package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentDetailsFragmentBinding

class UnifiedCommentDetailsFragment : Fragment(R.layout.unified_comment_details_fragment) {
    private var binding: UnifiedCommentDetailsFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UnifiedCommentDetailsFragmentBinding.bind(view).apply {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance() = UnifiedCommentDetailsFragment().apply {
            arguments = Bundle().apply {
            }
        }
    }
}
