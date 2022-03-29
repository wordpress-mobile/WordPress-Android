package org.wordpress.android.ui.featureintroduction

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.FeatureIntroductionDialogFragmentBinding
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor

abstract class FeatureIntroductionDialogFragment : DialogFragment() {

    private var _binding: FeatureIntroductionDialogFragmentBinding? = null
    private val binding get() = _binding ?: throw NullPointerException("_binding cannot be null")

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
        _binding = FeatureIntroductionDialogFragmentBinding.bind(view)
        setupCloseButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setPrimaryButtonListener(listener: () -> Unit) {
        binding.primaryButton.setOnClickListener { listener() }
    }

    fun setPrimaryButtonText(@StringRes textRes: Int) {
        binding.primaryButton.text = getString(textRes)
    }

    fun setSecondaryButtonListener(listener: () -> Unit) {
        binding.secondaryButton.setOnClickListener { listener() }
    }

    fun setSecondaryButtonText(@StringRes textRes: Int) {
        binding.secondaryButton.text = getString(textRes)
    }

    fun setHeaderTitle(@StringRes headerTitleRes: Int) {
        binding.headerTitle.text = getString(headerTitleRes)
    }

    fun setHeaderIcon(@DrawableRes headerIconRes: Int) {
        binding.headerIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, headerIconRes, context?.theme))
    }

    fun setContent(content: @Composable () -> Unit) {
        binding.content.setContent(content)
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener { dismiss() }
    }
}
