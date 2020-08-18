package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fullscreen_error_with_retry.*
import kotlinx.android.synthetic.main.reader_interests_fragment_layout.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialLoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.WPActivityUtils
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class ReaderInterestsFragment : Fragment(R.layout.reader_interests_fragment_layout) {
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderInterestsViewModel
    private lateinit var parentViewModel: ReaderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WPActivityUtils.setLightStatusBar(requireActivity().window, true)
        initDoneButton()
        initRetryButton()
        initViewModel()
    }

    private fun initDoneButton() {
        done_button.setOnClickListener {
            viewModel.onDoneButtonClick()
        }
    }

    private fun initRetryButton() {
        error_retry.setOnClickListener {
            viewModel.onRetryButtonClick()
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderInterestsViewModel::class.java)
        parentViewModel = ViewModelProviders.of(requireParentFragment()).get(ReaderViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            when (uiState) {
                is InitialLoadingUiState -> {
                }
                is ContentUiState -> {
                    updateInterests(uiState.interestsUiState)
                }
                is ErrorUiState -> {
                    updateErrorLayout(uiState)
                }
            }
            updateDoneButton(uiState.doneButtonUiState)
            with(uiHelpers) {
                updateVisibility(progress_bar, uiState.progressBarVisible)
                updateVisibility(title, uiState.titleVisible)
                updateVisibility(subtitle, uiState.subtitleVisible)
                updateVisibility(error_layout, uiState.errorLayoutVisible)
            }
        })

        viewModel.snackbarEvents.observe(viewLifecycleOwner, Observer {
            it?.applyIfNotHandled {
                showSnackbar()
            }
        })

        viewModel.start(parentViewModel, LocaleManager.getLanguage(WordPress.getContext()))
    }

    private fun updateDoneButton(doneButtonUiState: DoneButtonUiState) {
        with(done_button) {
            isEnabled = doneButtonUiState.enabled
            text = getString(doneButtonUiState.titleRes)
        }
        uiHelpers.updateVisibility(done_button, doneButtonUiState.visible)
    }

    private fun updateInterests(interestsUiState: List<TagUiState>) {
        interestsUiState.forEachIndexed { index, interestTagUiState ->
            val chip = interests_chip_group.findViewWithTag(interestTagUiState.slug)
                ?: createChipView(interestTagUiState.slug, index)
            with(chip) {
                text = interestTagUiState.title
                isChecked = interestTagUiState.isChecked
            }
        }
    }

    private fun updateErrorLayout(uiState: ErrorUiState) {
        with(uiHelpers) {
            setTextOrHide(error_title, uiState.titleResId)
            setTextOrHide(error_subtitle, uiState.subtitleResId)
            updateVisibility(contact_support, uiState.showContactSupport)
        }
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val snackbar = WPSnackbar.make(bottom_bar, getString(this.messageRes), Snackbar.LENGTH_LONG)
        if (this.buttonTitleRes != null) {
            snackbar.setAction(getString(this.buttonTitleRes)) {
                this.buttonAction.invoke()
            }
        }
        snackbar.setAnchorView(bottom_bar)
        snackbar.show()
    }

    private fun createChipView(slug: String, index: Int): Chip {
        val chip = layoutInflater.inflate(
            R.layout.reader_interest_filter_chip,
            interests_chip_group,
            false
        ) as Chip
        with(chip) {
            tag = slug
            setOnCheckedChangeListener { compoundButton, isChecked ->
                if (compoundButton.isPressed) {
                    viewModel.onInterestAtIndexToggled(index, isChecked)
                }
            }
            interests_chip_group.addView(chip)
        }
        return chip
    }

    override fun onDestroyView() {
        WPActivityUtils.setLightStatusBar(requireActivity().window, false)
        super.onDestroyView()
    }

    companion object {
        const val TAG = "reader_interests_fragment_tag"
    }
}
