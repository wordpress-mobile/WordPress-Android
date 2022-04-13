package org.wordpress.android.ui.sitecreation.sitename

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isInvisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteCreationSiteNameFragmentBinding
import org.wordpress.android.ui.sitecreation.sitename.SiteCreationSiteNameViewModel.SiteNameUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtilsWrapper
import javax.inject.Inject

/**
 * Implements the Site Name UI
 */
@Suppress("TooManyFunctions")
class SiteCreationSiteNameFragment : Fragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelper: UiHelpers
    @Inject internal lateinit var displayUtils: DisplayUtilsWrapper

    private lateinit var viewModel: SiteCreationSiteNameViewModel
    private var binding: SiteCreationSiteNameFragmentBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.site_creation_site_name_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(SiteCreationSiteNameViewModel::class.java)

        val binding = SiteCreationSiteNameFragmentBinding.bind(view)
        this.binding = binding
        with(binding) {
            setupUi()
            setupViewModel()
            setupActionListeners()
        }
    }

    private fun SiteCreationSiteNameFragmentBinding.setupUi() {
        siteCreationSiteNameHeader.title?.setText(R.string.new_site_creation_site_name_header_title)
        siteCreationSiteNameHeader.subtitle?.setText(R.string.new_site_creation_site_name_header_subtitle)
        siteCreationSiteNameTitlebar.appBarTitle.setText(R.string.new_site_creation_site_name_title)
        siteCreationSiteNameTitlebar.appBarTitle.isInvisible = !isPhoneLandscape()
        viewModel.uiState.value?.siteName.let { input.setText(it) }
        input.requestFocus()
        ActivityUtils.showKeyboard(input)
    }

    private fun SiteCreationSiteNameFragmentBinding.setupViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { updateUiState(it) }
        viewModel.onSiteNameEntered.observe(viewLifecycleOwner, (requireActivity() as
                SiteNameScreenListener)::onSiteNameEntered)
        viewModel.start()
    }

    private fun SiteCreationSiteNameFragmentBinding.setupActionListeners() {
        siteCreationSiteNameTitlebar.skipButton.setOnClickListener { viewModel.onSkipPressed() }
        siteCreationSiteNameTitlebar.backButton.setOnClickListener { viewModel.onBackPressed() }
        continueButton.setOnClickListener {
            viewModel.onSiteNameEntered()
        }
        input.doOnTextChanged { text, _, _, _ ->
            viewModel.onSiteNameChanged(text?.toString() ?: "")
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    viewModel.onSiteNameEntered()
                    return@setOnEditorActionListener true
                }
                else -> return@setOnEditorActionListener false
            }
        }
    }

    private fun SiteCreationSiteNameFragmentBinding.updateUiState(uiState: SiteNameUiState) {
        // Using isVisible would make the condition easier read, But it produces a UI bug causing
        // the continue button to briefly appear the first time a character is entered in the input.
        continueButtonContainer.isInvisible = !uiState.isContinueButtonEnabled || isPhoneLandscape()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun isPhoneLandscape() = displayUtils.isLandscapeBySize() && !displayUtils.isTablet()

    companion object {
        const val TAG = "site_creation_site_name_fragment_tag"
    }
}
