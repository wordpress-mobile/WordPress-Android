package org.wordpress.android.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.databinding.CommentModerationBinding
import org.wordpress.android.util.extensions.getSerializableCompat
import java.io.Serializable

class ModerationBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var binding: CommentModerationBinding? = null
    private val state by lazy {
        arguments?.getSerializableCompat<CommentState>(KEY_STATE)
            ?: throw IllegalArgumentException("CommentState not provided")
    }

    var onApprovedClicked = {}
    var onPendingClicked = {}
    var onSpamClicked = {}
    var onTrashClicked = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        CommentModerationBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // define the peekHeight to avoid it expanding partially
        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val metrics = resources.displayMetrics
                behavior.peekHeight = metrics.heightPixels
            }
        }

        binding?.setupLayout()
    }

    private fun CommentModerationBinding.setupLayout() {
        // handle visibilities
        buttonApprove.isVisible = state.canModerate
        buttonPending.isVisible = state.canModerate
        buttonSpam.isVisible = state.canMarkAsSpam
        buttonTrash.isVisible = state.canTrash

        // handle clicks
        buttonApprove.setOnClickListener { onApprovedClicked() }
        buttonPending.setOnClickListener { onPendingClicked }
        buttonSpam.setOnClickListener { onSpamClicked() }
        buttonTrash.setOnClickListener { onTrashClicked }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ModerationBottomSheetDialogFragment"
        private const val KEY_STATE = "state"
        fun newInstance(state: CommentState) = ModerationBottomSheetDialogFragment()
            .apply {
                arguments = Bundle().apply { putSerializable(KEY_STATE, state) }
            }
    }

    /**
     * For handling the UI state of the comment moderation bottom sheet
     */
    data class CommentState(
        val canModerate: Boolean,
        val canTrash: Boolean,
        val canMarkAsSpam: Boolean,
    ) : Serializable
}
