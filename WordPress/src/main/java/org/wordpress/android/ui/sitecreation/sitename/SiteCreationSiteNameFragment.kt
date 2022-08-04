package org.wordpress.android.ui.sitecreation.sitename

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isInvisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.R.string
import org.wordpress.android.databinding.SiteCreationSiteNameFragmentBinding
import org.wordpress.android.ui.sitecreation.sitename.SiteCreationSiteNameViewModel.SiteNameUiState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.HtmlUtils
import javax.inject.Inject

/**
 * Implements the Site Name UI
 */
@AndroidEntryPoint
class SiteCreationSiteNameFragment : Fragment() {
    @Inject internal lateinit var uiHelper: UiHelpers
    @Inject internal lateinit var displayUtils: DisplayUtilsWrapper
    @Inject internal lateinit var htmlMessageUtils: HtmlMessageUtils

    private val viewModel: SiteCreationSiteNameViewModel by activityViewModels()
    private var binding: SiteCreationSiteNameFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.site_creation_site_name_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = SiteCreationSiteNameFragmentBinding.bind(view)
        this.binding = binding
        with(binding) {
            setupUi()
            setupViewModel()
            setupActionListeners()
        }
    }

    private val siteIntent: String?
        get() = arguments?.getString(ARG_SITE_INTENT)

    private val headerTitleWithIntentColoredBlueIfSpecified: CharSequence
        get() {
            val blueColorHexCode = HtmlUtils.colorResToHtmlColor(requireContext(), color.blue)
            return htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                    string.new_site_creation_site_name_header_title,
                    siteIntent?.let { "<span style='color:$blueColorHexCode;'>$it</span>" }.orEmpty()
            )
        }

    private fun SiteCreationSiteNameFragmentBinding.setupUi() {
        siteCreationSiteNameHeader.title?.text = headerTitleWithIntentColoredBlueIfSpecified
        siteCreationSiteNameHeader.subtitle?.setText(R.string.new_site_creation_site_name_header_subtitle)
        siteCreationSiteNameTitlebar.appBarTitle.setText(R.string.new_site_creation_site_name_title)
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
        continueButtonContainer.isInvisible = !uiState.isContinueButtonEnabled || displayUtils.isPhoneLandscape()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_SITE_INTENT = "arg_site_intent"

        fun newInstance(siteIntent: String?): SiteCreationSiteNameFragment {
            val bundle = Bundle().apply {
                putString(ARG_SITE_INTENT, siteIntent)
            }

            return SiteCreationSiteNameFragment().apply {
                arguments = bundle
            }
        }

        const val TAG = "site_creation_site_name_fragment_tag"
    }
}
