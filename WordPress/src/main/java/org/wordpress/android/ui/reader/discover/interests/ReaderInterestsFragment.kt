package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderFullscreenErrorWithRetryBinding
import org.wordpress.android.databinding.ReaderInterestFilterChipBinding
import org.wordpress.android.databinding.ReaderInterestsFragmentLayoutBinding
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialLoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class ReaderInterestsFragment : Fragment(R.layout.reader_interests_fragment_layout) {
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderInterestsViewModel
    private var parentViewModel: ReaderViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val entryPoint = requireActivity().intent.getSerializableExtra(READER_INTEREST_ENTRY_POINT) as? EntryPoint
                ?: EntryPoint.DISCOVER
        val binding = ReaderInterestsFragmentLayoutBinding.bind(view)
        initDoneButton(binding)
        initRetryButton(binding.includeErrorLayout)
        initBackButton(entryPoint, binding)
        initViewModel(entryPoint, binding)
    }

    private fun initDoneButton(binding: ReaderInterestsFragmentLayoutBinding) = with(binding) {
        doneButton.setOnClickListener {
            viewModel.onDoneButtonClick()
        }
    }

    private fun initRetryButton(binding: ReaderFullscreenErrorWithRetryBinding) = with(binding) {
        errorRetry.setOnClickListener {
            viewModel.onRetryButtonClick()
        }
    }

    private fun initBackButton(entryPoint: EntryPoint, binding: ReaderInterestsFragmentLayoutBinding) = with(binding) {
        if (entryPoint == EntryPoint.DISCOVER) {
            backButton.visibility = View.VISIBLE
            backButton.setOnClickListener {
                viewModel.onBackButtonClick()
            }
        }
    }

    private fun initViewModel(entryPoint: EntryPoint, binding: ReaderInterestsFragmentLayoutBinding) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ReaderInterestsViewModel::class.java)
        if (entryPoint == EntryPoint.DISCOVER) {
            parentViewModel = ViewModelProvider(requireParentFragment()).get(ReaderViewModel::class.java)
        }
        startObserving(entryPoint, binding)
    }

    private fun startObserving(entryPoint: EntryPoint, binding: ReaderInterestsFragmentLayoutBinding) = with(binding) {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is InitialLoadingUiState -> {
                }
                is ContentUiState -> {
                    updateInterests(uiState.interestsUiState, binding.interestsChipGroup)
                }
                is ErrorUiState -> {
                    updateErrorLayout(uiState, binding.includeErrorLayout)
                }
            }
            updateDoneButton(uiState.doneButtonUiState, binding.doneButton)
            with(uiHelpers) {
                updateVisibility(progressBar, uiState.progressBarVisible)
                updateVisibility(title, uiState.titleVisible)
                updateVisibility(includeErrorLayout.errorLayout, uiState.errorLayoutVisible)
            }
        }

        viewModel.snackbarEvents.observe(viewLifecycleOwner) {
            it?.applyIfNotHandled {
                showSnackbar(binding.bottomBar)
            }
        }

        viewModel.closeReaderInterests.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let {
                requireActivity().finish()
            }
        }

        viewModel.start(
                LocaleManager.getLanguage(WordPress.getContext()),
                parentViewModel,
                entryPoint
        )
    }

    private fun updateDoneButton(doneButtonUiState: DoneButtonUiState, doneButton: Button) {
        with(doneButton) {
            isEnabled = doneButtonUiState.enabled
            text = getString(doneButtonUiState.titleRes)
        }
        uiHelpers.updateVisibility(doneButton, doneButtonUiState.visible)
    }

    private fun updateInterests(interestsUiState: List<TagUiState>, interestsChipGroup: ChipGroup) {
        interestsUiState.forEachIndexed { index, interestTagUiState ->
            val chip = interestsChipGroup.findViewWithTag(interestTagUiState.slug)
                    ?: createChipView(interestTagUiState.slug, index, interestsChipGroup)
            with(chip) {
                text = interestTagUiState.title
                isChecked = interestTagUiState.isChecked
            }
        }
    }

    private fun updateErrorLayout(uiState: ErrorUiState, binding: ReaderFullscreenErrorWithRetryBinding) {
        with(uiHelpers) {
            setTextOrHide(binding.errorTitle, uiState.titleRes)
        }
    }

    private fun SnackbarMessageHolder.showSnackbar(bottomBar: MaterialCardView) {
        val snackbar = WPSnackbar.make(
                bottomBar,
                uiHelpers.getTextOfUiString(requireContext(), this.message),
                Snackbar.LENGTH_LONG
        )
        if (this.buttonTitle != null) {
            snackbar.setAction(uiHelpers.getTextOfUiString(requireContext(), this.buttonTitle)) {
                this.buttonAction.invoke()
            }
        }
        snackbar.anchorView = bottomBar
        snackbar.show()
    }

    private fun createChipView(slug: String, index: Int, interestsChipGroup: ChipGroup): Chip {
        val chip = ReaderInterestFilterChipBinding.inflate(layoutInflater, interestsChipGroup, false).root
        with(chip) {
            tag = slug
            setOnCheckedChangeListener { compoundButton, isChecked ->
                if (compoundButton.isPressed) {
                    viewModel.onInterestAtIndexToggled(index, isChecked)
                }
            }
            interestsChipGroup.addView(this)
        }
        return chip
    }

    companion object {
        const val TAG = "reader_interests_fragment_tag"
        const val READER_INTEREST_ENTRY_POINT = "reader_interest_entry_point"
    }

    enum class EntryPoint {
        DISCOVER,
        SETTINGS
    }
}
