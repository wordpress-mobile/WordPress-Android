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
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection.KeepCurrentHomepage
import org.wordpress.android.viewmodel.themes.ThemesViewModel.BottomSheetUIState.Selection.UseThemeHomepage
import javax.inject.Inject

class ThemeBottomSheetFragment : BottomSheetDialogFragment() {
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

            viewModel.bottomSheetUiState.observe(this@ThemeBottomSheetFragment) {
                themeName.text = it.themeNameText
                sheetInfo.text = it.sheetInfoText
                useThemeHomepageOption.text = it.useThemeHomePageOptionText

                when(it.selection){
                    is UseThemeHomepage -> toggleSelection(useThemeCheck, keepCurrentCheck)
                    is KeepCurrentHomepage -> toggleSelection(keepCurrentCheck, useThemeCheck)
                }
            }

            closeButton.setOnClickListener { viewModel.onDismissButtonClicked() }
            useThemeOptionLayout.setOnClickListener { viewModel.onUseThemeHomepageSelected() }
            keepCurrentOptionLayout.setOnClickListener { viewModel.onKeepCurrentHomepageSelected() }
            previewThemeButton.setOnClickListener { viewModel.onPreviewButtonClicked() }
            activateThemeButton.setOnClickListener { viewModel.onActivateButtonClicked() }
        }
    }

    override fun getTheme(): Int  = R.style.ThemeActivationBottomSheetStyle

    private fun toggleSelection(@NonNull viewTapped: View, vararg otherViews: View) {
        viewTapped.visibility = VISIBLE
        otherViews.forEach { it.visibility = INVISIBLE }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
