package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.reader_interests_fragment_layout.*
import kotlinx.android.synthetic.main.fullscreen_error_with_retry.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.ReaderFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.InterestUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadFailedUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadSuccessUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialUiState
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class ReaderInterestsFragment : Fragment(R.layout.reader_interests_fragment_layout) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderInterestsViewModel
    @Inject lateinit var uiHelpers: UiHelpers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        error_retry.setOnClickListener { // TODO
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ReaderInterestsViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            when (uiState) {
                is InitialUiState -> {
                }
                is ContentLoadSuccessUiState -> {
                    updateInterests(uiState.interestsUiState)
                }
                is ContentLoadFailedUiState -> {
                    updateErrorLayout(uiState)
                }
            }
            updateDoneButton(uiState.doneButtonUiState)
            with(uiHelpers) {
                updateVisibility(progress_bar, uiState.progressBarVisible)
                updateVisibility(title, uiState.titleVisible)
                updateVisibility(subtitle, uiState.subtitleVisible)
                updateVisibility(error_layout, uiState.fullscreenErrorLayoutVisible)
            }
        })

        viewModel.navigateToDiscover.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                navigateToDiscover()
            }
        })

        viewModel.start()
    }

    private fun updateDoneButton(doneButtonUiState: DoneButtonUiState) {
        with(done_button) {
            isEnabled = doneButtonUiState.enabled
            text = getString(doneButtonUiState.titleRes)
        }
        uiHelpers.updateVisibility(done_button, doneButtonUiState.visible)
    }

    private fun updateInterests(interestsUiState: List<InterestUiState>) {
        interestsUiState.forEachIndexed { index, interestTagUiState ->
            val chip = interests_chip_group.findViewWithTag(interestTagUiState.title)
                ?: createChipView(interestTagUiState.title, index)
            with(chip) {
                text = interestTagUiState.title
                isChecked = interestTagUiState.isChecked
            }
        }
    }

    private fun updateErrorLayout(uiState: ContentLoadFailedUiState) {
        with(uiHelpers) {
            setTextOrHide(error_title, uiState.titleResId)
            setTextOrHide(error_subtitle, uiState.subtitleResId)
            updateVisibility(contact_support, uiState.showContactSupport)
        }
    }

    private fun createChipView(titleTag: String, index: Int): Chip {
        val chip = layoutInflater.inflate(
            R.layout.reader_interest_filter_chip,
            interests_chip_group,
            false
        ) as Chip
        with(chip) {
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
            tag = titleTag
            setOnCheckedChangeListener { compoundButton, isChecked ->
                if (compoundButton.isPressed) {
                    viewModel.onInterestAtIndexToggled(index, isChecked)
                }
            }
            interests_chip_group.addView(chip)
        }
        return chip
    }

    private fun navigateToDiscover() {
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
            R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
        )
        fragmentTransaction.replace(R.id.fragment_container, ReaderFragment(), tag).commit()
    }
}
