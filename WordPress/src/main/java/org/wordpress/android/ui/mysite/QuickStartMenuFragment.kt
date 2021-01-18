package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.quick_start_menu_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class QuickStartMenuFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: QuickStartMenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(QuickStartMenuViewModel::class.java)
        viewModel.id = requireArguments().getString(ID_ARG_KEY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.quick_start_menu_fragment, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pin_action.setOnClickListener { invokeAndDismiss { viewModel.onPinActionClicked() } }
        hide_action.setOnClickListener { invokeAndDismiss { viewModel.onHideActionClicked() } }
        remove_action.setOnClickListener { invokeAndDismiss { viewModel.onRemoveActionClicked() } }

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun invokeAndDismiss(action: () -> Unit) {
        action.invoke()
        dismiss()
    }

    companion object {
        const val ID_ARG_KEY = "ID_ARG_KEY"

        fun newInstance(id: String) = QuickStartMenuFragment().apply {
            arguments = Bundle().apply {
                putString(ID_ARG_KEY, id)
            }
        }
    }
}
