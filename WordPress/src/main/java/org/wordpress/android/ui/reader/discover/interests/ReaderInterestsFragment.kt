package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.reader_interests_fragment_layout.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.ReaderFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.InterestUiState
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
        initViewModel()
    }

    private fun initDoneButton() {
        done_button.setOnClickListener {
            viewModel.onDoneButtonClick()
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ReaderInterestsViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            updateInterests(uiState.interestsUiState)
            updateDoneButton(uiState.doneButtonUiState)
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
            var chip = interests_chip_group.findViewWithTag<Chip>(interestTagUiState.title)
            if (chip == null) {
                chip = layoutInflater.inflate(
                    R.layout.reader_interest_filter_chip,
                    interests_chip_group,
                    false
                ) as Chip
                with(chip) {
                    layoutDirection = View.LAYOUT_DIRECTION_LOCALE
                    tag = interestTagUiState.title
                    setOnCheckedChangeListener { compoundButton, isChecked ->
                        if (compoundButton.isPressed) {
                            viewModel.onInterestAtIndexToggled(index, isChecked)
                        }
                    }
                    interests_chip_group.addView(chip)
                }
            }
            with(chip) {
                text = interestTagUiState.title
                isChecked = interestTagUiState.isChecked
            }
        }
    }

    private fun navigateToDiscover() {
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.anim.fragment_close_enter, R.anim.fragment_close_exit,
            R.anim.fragment_close_enter, R.anim.fragment_close_exit
        )
        fragmentTransaction.replace(R.id.fragment_container, ReaderFragment(), tag).commit()
    }
}
