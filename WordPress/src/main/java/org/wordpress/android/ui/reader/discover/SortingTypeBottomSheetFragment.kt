package org.wordpress.android.ui.reader.discover

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.reader_discover_sorting_bottom_sheet.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.discover.DiscoverSortingType.POPULARITY
import org.wordpress.android.ui.reader.discover.DiscoverSortingType.TIME
import javax.inject.Inject

class SortingTypeBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderDiscoverViewModel

    companion object {
        const val TAG = "sorting_type_bottom_sheet_fragment"
        fun getInstance(): BottomSheetDialogFragment {
            return SortingTypeBottomSheetFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.reader_discover_sorting_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(parentFragment as ViewModelStoreOwner, viewModelFactory)
                .get(ReaderDiscoverViewModel::class.java)

        val currentSortingType = viewModel.sortingType.value
        checkedSelectedOption(currentSortingType)

        container_sorting_type.setOnCheckedChangeListener { _, checkedId ->
            val sortingType = getSortingType(checkedId)
            viewModel.onSortingTypeChose(sortingType)
        }

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val metrics = resources.displayMetrics
                behavior.peekHeight = metrics.heightPixels / 2
            }
        }
    }

    private fun checkedSelectedOption(currentSortingType: DiscoverSortingType?) {
        val option = when (currentSortingType) {
            TIME -> choice_recent
            POPULARITY -> choice_popular
            else -> null
        }
        option?.isChecked = true
    }

    private fun getSortingType(checkedId: Int): DiscoverSortingType {
        return when (checkedId) {
            R.id.choice_popular -> POPULARITY
            R.id.choice_recent -> TIME
            else -> throw Throwable("Unsupported sorting type")
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.onSortingDialogCancel()
    }
}
