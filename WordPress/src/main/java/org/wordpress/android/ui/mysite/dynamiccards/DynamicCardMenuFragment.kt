package org.wordpress.android.ui.mysite.dynamiccards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.QuickStartMenuFragmentBinding
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class DynamicCardMenuFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var imageManager: ImageManager
    private lateinit var viewModel: DynamicCardMenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(DynamicCardMenuViewModel::class.java)
        viewModel.start(
            requireNotNull(requireArguments().getString(ID_ARG_KEY)),
            requireArguments().getBoolean(IS_PINNED_KEY)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.quick_start_menu_fragment, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(QuickStartMenuFragmentBinding.bind(view)) {
            val quickStartCardMenuPin = if (viewModel.isPinned) {
                R.string.quick_start_card_menu_unpin
            } else {
                R.string.quick_start_card_menu_pin
            }
            quickStartPinText.setText(quickStartCardMenuPin)
            pinAction.setOnClickListener { invokeAndDismiss { viewModel.onPinActionClicked() } }
            hideAction.setOnClickListener { invokeAndDismiss { viewModel.onHideActionClicked() } }
            removeAction.setOnClickListener { invokeAndDismiss { viewModel.onRemoveActionClicked() } }
        }

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
        const val IS_PINNED_KEY = "PINNED_KEY"

        fun newInstance(cardType: DynamicCardType, isPinned: Boolean) = DynamicCardMenuFragment().apply {
            arguments = Bundle().apply {
                putString(ID_ARG_KEY, cardType.name)
                putBoolean(IS_PINNED_KEY, isPinned)
            }
        }
    }

    data class DynamicCardMenuModel(val cardType: DynamicCardType, val isPinned: Boolean) {
        val id = cardType.name
    }
}
