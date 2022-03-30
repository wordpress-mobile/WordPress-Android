package org.wordpress.android.ui.sitecreation.verticals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
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
@Suppress("TooManyFunctions")
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // We add the textChanged listener after the viewState has been restored otherwise the viewModel callback
        // gets called when the system restores the value to the EditText
        // which results in an unwanted uiState update.
        binding?.addSearchTextChangedListener()
    }

    private fun SiteCreationIntentsFragmentBinding.setupUi() {
        siteCreationIntentsTitlebar.appBarTitle.isInvisible = !isPhoneLandscape()
        recyclerView.adapter = SiteCreationIntentsAdapter(uiHelper)
    }

    private fun SiteCreationIntentsFragmentBinding.updateUiState(uiState: IntentsUiState) {
        (recyclerView.adapter as SiteCreationIntentsAdapter).update(uiState.content.items)
        updateTitleVisibility(uiState.isAppBarTitleVisible)
        if (!uiState.isHeaderVisible) input.requestFocus()
        animateHeaderVisibility(uiState.isHeaderVisible)
        updateContinueButtonVisibility(uiState.isContinueButtonVisible)
    }

    private fun SiteCreationIntentsFragmentBinding.updateTitleVisibility(shouldAppBarTitleBeVisible: Boolean) {
        // In landscape mode this code doesn't apply, since the header texts are not in the layout
        uiHelper.fadeInfadeOutViews(
                siteCreationIntentsTitlebar.appBarTitle,
                siteCreationIntentsHeader.title,
                shouldAppBarTitleBeVisible
        )
    }

    private fun SiteCreationIntentsFragmentBinding.updateContinueButtonVisibility(shouldBeVisible: Boolean) {
        continueButtonContainer.isVisible = shouldBeVisible
        continueButtonShadow.isVisible = shouldBeVisible
    }

    private fun SiteCreationIntentsFragmentBinding.animateHeaderVisibility(shouldBeVisible: Boolean) {
        val headerLayout = siteCreationIntentsHeader.root
        val toggleVisibility = Runnable { headerLayout.isVisible = shouldBeVisible }

        when {
            !shouldBeVisible && headerLayout.isVisible -> {
                headerLayout.animate().translationY(-headerLayout.height.toFloat()).withEndAction(toggleVisibility)
            }
            shouldBeVisible && headerLayout.isGone -> {
                headerLayout.animate().translationY(0f).withEndAction(toggleVisibility)
            }
        }
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
        continueButton.setOnClickListener { viewModel.onContinuePressed() }
        setScrollListener()
        input.setOnFocusChangeListener { _, willFocus -> if (willFocus) viewModel.onSearchInputFocused() }
    }

    private fun SiteCreationIntentsFragmentBinding.addSearchTextChangedListener() {
        input.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchTextChanged(text?.toString() ?: "")
        }
    }

    private fun SiteCreationIntentsFragmentBinding.setScrollListener() {
        val scrollThreshold = resources.getDimension(R.dimen.siq_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
        viewModel.onAppBarOffsetChanged(verticalOffset = 0, scrollThreshold)
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
