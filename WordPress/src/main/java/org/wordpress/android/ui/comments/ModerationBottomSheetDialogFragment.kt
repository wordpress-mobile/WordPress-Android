package org.wordpress.android.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.databinding.CommentModerationBinding

class ModerationBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var binding: CommentModerationBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        CommentModerationBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ModerationBottomSheetDialogFragment"
        fun newInstance() = ModerationBottomSheetDialogFragment()
    }
}
