package org.wordpress.android.ui.themes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ThemeActivationBottomSheetBinding
import org.wordpress.android.viewmodel.themes.ThemesViewModel
import org.wordpress.android.viewmodel.themes.ThemesViewModel.Selection.KEEP_CURRENT_HOMEPAGE
import org.wordpress.android.viewmodel.themes.ThemesViewModel.Selection.USE_THEME_HOMEPAGE
import javax.inject.Inject

class ThemeActivationBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ThemesViewModel

    companion object {
        const val TAG = "ThemeActivationBottomSheetFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.theme_activation_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensures that bottom sheet always opens in expanded state even in landscape mode
        from(requireView().parent as View).state = STATE_EXPANDED

        with(ThemeActivationBottomSheetBinding.bind(view)) {
            viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(ThemesViewModel::class.java)
            viewModel.themeToActivate.observe(this@ThemeActivationBottomSheetFragment) {
                themeNameTv.text = it.name
                infoTv.text = requireActivity().getString(R.string.theme_bottom_sheet_info, it.name)
                useThemeLayoutMainTextTv.text =
                        requireActivity().getString(R.string.theme_bottom_sheet_use_theme_layout_main_text, it.name)
            }

            closeButton.setOnClickListener { dismiss() }

            useThemeLayoutLl.setOnClickListener {
                toggleSelection(useThemeCheckIv, keepCurrentCheckIv)
                viewModel.bottomSheetSelection = USE_THEME_HOMEPAGE
            }

            keepCurrentLayoutLl.setOnClickListener {
                toggleSelection(keepCurrentCheckIv, useThemeCheckIv)
                viewModel.bottomSheetSelection = KEEP_CURRENT_HOMEPAGE
            }

            previewThemeButton.setOnClickListener { /*TODO*/ }
            activateThemeButton.setOnClickListener { /*TODO*/ }
        }
    }

    private fun toggleSelection(@NonNull viewTapped: View, vararg otherView: View) {
        if (viewTapped.visibility == INVISIBLE) {
            viewTapped.visibility = VISIBLE
            otherView.forEach { it.visibility = INVISIBLE }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
