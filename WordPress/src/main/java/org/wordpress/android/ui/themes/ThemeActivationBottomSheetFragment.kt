package org.wordpress.android.ui.themes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.NonNull
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.ThemeActivationBottomSheetBinding

class ThemeActivationBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.theme_activation_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(ThemeActivationBottomSheetBinding.bind(view)) {
            closeButton.setOnClickListener { dismiss() }
            useThemeLayoutLl.setOnClickListener { toggleSelection(useThemeCheckIv, keepCurrentCheckIv) }
            keepCurrentLayoutLl.setOnClickListener { toggleSelection(keepCurrentCheckIv, useThemeCheckIv) }
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
}
