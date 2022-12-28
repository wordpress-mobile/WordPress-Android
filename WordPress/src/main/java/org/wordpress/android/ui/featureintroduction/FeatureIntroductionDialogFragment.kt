package org.wordpress.android.ui.featureintroduction

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.FeatureIntroductionDialogFragmentBinding
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor
import javax.inject.Inject

abstract class FeatureIntroductionDialogFragment : DialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: FeatureIntroductionViewModel
    private var _binding: FeatureIntroductionDialogFragmentBinding? = null
    private val binding get() = _binding ?: throw NullPointerException("_binding cannot be null")

    override fun getTheme(): Int {
        return R.style.FeatureIntroductionDialogFragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                viewModel.onBackButtonClick()
                super.onBackPressed()
            }
        }.apply {
            setStatusBarAsSurfaceColor()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FeatureIntroductionDialogFragmentBinding.inflate(inflater).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(FeatureIntroductionViewModel::class.java)
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

    fun togglePrimaryButtonVisibility(isVisible: Boolean) {
        uiHelpers.updateVisibility(binding.primaryButton, isVisible)
    }

    fun setSecondaryButtonListener(listener: () -> Unit) {
        binding.secondaryButton.setOnClickListener { listener() }
    }

    fun setSecondaryButtonText(@StringRes textRes: Int) {
        binding.secondaryButton.text = getString(textRes)
    }

    fun setCloseButtonListener(listener: () -> Unit) {
        binding.closeButton.setOnClickListener { listener() }
    }

    fun toggleSecondaryButtonVisibility(isVisible: Boolean) {
        uiHelpers.updateVisibility(binding.secondaryButton, isVisible)
    }

    fun setHeaderTitle(@StringRes headerTitleRes: Int) {
        binding.headerTitle.text = getString(headerTitleRes)
    }

    fun setHeaderIcon(@DrawableRes headerIconRes: Int) {
        binding.headerIcon.setImageResource(headerIconRes)
    }

    fun setContent(view: View) {
        binding.contentContainer.addView(view)
    }

    fun setDismissAnalyticsEvent(stat: Stat, properties: Map<String, Any?>) {
        viewModel.setDismissAnalyticsEvent(stat, properties)
    }

    private fun setupCloseButton() {
        binding.closeButton.setOnClickListener {
            dismiss()
            viewModel.onCloseButtonClick()
        }
    }

    fun getSuperBinding(): FeatureIntroductionDialogFragmentBinding {
        return binding
    }
}
