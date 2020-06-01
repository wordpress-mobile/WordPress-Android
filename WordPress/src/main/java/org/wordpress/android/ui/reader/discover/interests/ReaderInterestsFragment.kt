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
import org.wordpress.android.models.ReaderTagList
import javax.inject.Inject

class ReaderInterestsFragment : Fragment(R.layout.reader_interests_fragment_layout) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderInterestsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderInterestsViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            updateInterests(uiState.interests)
        })

        viewModel.start()
    }

    private fun updateInterests(interests: ReaderTagList) {
        interests_chip_group.removeAllViews()
        for (index in interests.indices) {
            val chip = layoutInflater.inflate(R.layout.reader_interest_filter_chip, interests_chip_group, false) as Chip
            chip.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
            chip.text = interests[index].tagTitle
            chip.tag = interests[index].tagSlug
            interests_chip_group.addView(chip)
        }
    }
}
