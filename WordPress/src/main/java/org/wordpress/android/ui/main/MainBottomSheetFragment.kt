package org.wordpress.android.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.RecyclerViewBottomSheetBinding
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import javax.inject.Inject

class MainBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: WPMainActivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.recycler_view_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(RecyclerViewBottomSheetBinding.bind(view)) {
            contentRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
            contentRecyclerView.adapter = AddContentAdapter(requireActivity())

            viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(WPMainActivityViewModel::class.java)
            viewModel.mainActions.observe(this@MainBottomSheetFragment) {
                (contentRecyclerView.adapter as? AddContentAdapter)?.update(it ?: listOf())
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
        viewModel.onMainBottomSheetCreated()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
