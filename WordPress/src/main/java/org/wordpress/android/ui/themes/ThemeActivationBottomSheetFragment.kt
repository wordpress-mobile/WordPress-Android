package org.wordpress.android.ui.themes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.NonNull
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.ThemeActivationBottomSheetBinding

class ThemeActivationBottomSheetFragment : BottomSheetDialogFragment() {
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

        // Ensures that bottom sheet always open in expanded state even in landscape mode
        val behavior = from(requireView().parent as View)
        behavior.state = STATE_EXPANDED

        with(ThemeActivationBottomSheetBinding.bind(view)) {
            closeButton.setOnClickListener { dismiss() }
            useThemeOptionLayout.setOnClickListener { toggleSelection(useThemeCheck, keepCurrentCheck) }
            keepCurrentOptionLayout.setOnClickListener { toggleSelection(keepCurrentCheck, useThemeCheck) }
            previewThemeButton.setOnClickListener { /*TODO*/ }
            activateThemeButton.setOnClickListener { /*TODO*/ }
        }
    }

    override fun getTheme(): Int  = R.style.ThemeActivationBottomSheetStyle

    private fun toggleSelection(@NonNull viewTapped: View, vararg otherView: View) {
        if (viewTapped.visibility == INVISIBLE) {
            viewTapped.visibility = VISIBLE
            otherView.forEach { it.visibility = INVISIBLE }
        }
    }
}
