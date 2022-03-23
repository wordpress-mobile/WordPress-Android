package org.wordpress.android.ui.featureintroduction

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.databinding.FeatureIntroductionDialogFragmentBinding
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor

abstract class FeatureIntroductionDialogFragment : DialogFragment() {
    abstract fun onPrimaryButtonClick()
    abstract fun onSecondaryButtonClick()
    abstract val headerTitle: String
    abstract val headerIcon: Drawable

    override fun getTheme(): Int {
        return R.style.FeatureIntroductionDialogFragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setStatusBarAsSurfaceColor()
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FeatureIntroductionDialogFragmentBinding.inflate(inflater).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FeatureIntroductionDialogFragmentBinding.bind(view)
        setupPrimaryButton(binding.primaryButton)
        setupSecondaryButton(binding.secondaryButton)
        setupHeaderTitle(binding.headerTitle)
        setupHeaderIcon(binding.headerIcon)
    }

    private fun setupPrimaryButton(primaryButton: MaterialButton) {
        primaryButton.setOnClickListener { onPrimaryButtonClick() }
    }

    private fun setupSecondaryButton(secondaryButton: MaterialButton) {
        secondaryButton.setOnClickListener { onSecondaryButtonClick() }
    }
}
