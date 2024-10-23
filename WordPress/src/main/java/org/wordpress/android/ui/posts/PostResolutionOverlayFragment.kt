package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.util.extensions.fillScreen
import javax.inject.Inject

@Suppress("DEPRECATION")
class PostResolutionOverlayFragment : BottomSheetDialogFragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: PostResolutionOverlayViewModel

    private var listener: PostResolutionOverlayListener? = null

    private val postModel: PostModel? by lazy {
        arguments?.getSerializable(ARG_POST_MODEL) as? PostModel
    }

    private val postResolutionType: PostResolutionType? by lazy {
        arguments?.getSerializable(ARG_POST_RESOLUTION_TYPE) as? PostResolutionType
    }

    @Suppress("TooGenericExceptionThrown")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PostResolutionOverlayListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement PostResolutionOverlayListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            (this as? BottomSheetDialog)?.fillScreen(isDraggable = true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initializeViewModelAndStart()
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeM2 {
                    val uiState by viewModel.uiState.observeAsState()
                    PostResolutionOverlay(uiState)
                }
            }
        }
    }

    private fun initializeViewModelAndStart() {
        viewModel = ViewModelProvider(this, viewModelFactory)[PostResolutionOverlayViewModel::class.java]

        viewModel.triggerListeners.observe(viewLifecycleOwner) {
            listener?.onPostResolutionConfirmed(it)
        }

        viewModel.dismissDialog.observe(viewLifecycleOwner) {
            dismiss()
        }

        viewModel.start(postModel, postResolutionType)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onDialogDismissed()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val TAG = "PostResolutionOverlayFragment"

        private const val ARG_POST_MODEL = "arg_post_model"
        private const val ARG_POST_RESOLUTION_TYPE = "arg_post_resolution_type"

        @JvmStatic
        fun newInstance(postModel: PostModel, postResolutionType: PostResolutionType) =
            PostResolutionOverlayFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_POST_MODEL, postModel)
                    putSerializable(ARG_POST_RESOLUTION_TYPE, postResolutionType)
                }
            }
    }
}
