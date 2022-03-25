package org.wordpress.android.ui.sitecreation.verticals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteCreationIntentsFragmentBinding
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentsUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtilsWrapper
import javax.inject.Inject

/**
 * Implements the Site Intent Question UI
 */
class SiteCreationIntentsFragment : Fragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelper: UiHelpers
    @Inject internal lateinit var displayUtils: DisplayUtilsWrapper

    private lateinit var viewModel: SiteCreationIntentsViewModel
    private var binding: SiteCreationIntentsFragmentBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.site_creation_intents_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(SiteCreationIntentsViewModel::class.java)

        val binding = SiteCreationIntentsFragmentBinding.bind(view)
        this.binding = binding
        with(binding) {
            setupUi()
            setupViewModel()
            setupActionListeners()
        }
    }

    private fun SiteCreationIntentsFragmentBinding.setupUi() {
        siteCreationIntentsTitlebar.appBarTitle.isInvisible = !isPhoneLandscape()
        siteCreationHeaderItem.title.setText(R.string.new_site_creation_intents_header_title)
        siteCreationHeaderItem.subtitle.setText(R.string.new_site_creation_intents_header_subtitle)
        recyclerView.adapter = SiteCreationIntentsAdapter(uiHelper)
    }

    private fun SiteCreationIntentsFragmentBinding.updateUiState(uiState: IntentsUiState) {
        (recyclerView.adapter as SiteCreationIntentsAdapter).update(uiState.items)
    }

    private fun SiteCreationIntentsFragmentBinding.setupViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { updateUiState(it) }
        viewModel.onIntentSelected.observe(viewLifecycleOwner, (requireActivity() as
                IntentsScreenListener)::onIntentSelected)
        viewModel.initializeFromResources(resources)
        viewModel.start()
    }

    private fun SiteCreationIntentsFragmentBinding.setupActionListeners() {
        siteCreationIntentsTitlebar.skipButton.setOnClickListener { viewModel.onSkipPressed() }
        siteCreationIntentsTitlebar.backButton.setOnClickListener { viewModel.onBackPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun isPhoneLandscape() = displayUtils.isLandscapeBySize() && !displayUtils.isTablet()

    companion object {
        const val TAG = "site_creation_intents_fragment_tag"
    }
}
