package org.wordpress.android.ui.stats.intro

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsRevampV2FeaturesIntroContentViewBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.featureintroduction.FeatureIntroductionDialogFragment
import org.wordpress.android.ui.stats.intro.StatsNewFeaturesIntroAction.DismissDialog
import org.wordpress.android.ui.stats.intro.StatsNewFeaturesIntroAction.OpenStats
import org.wordpress.android.util.extensions.exhaustive

class StatsNewFeaturesIntroDialogFragment : FeatureIntroductionDialogFragment() {
    private lateinit var viewModel: StatsNewFeatureIntroViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(StatsNewFeatureIntroViewModel::class.java)
        initializeViews()
        setupObservers()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    private fun initializeViews() {
        setHeaderIcon(R.drawable.ic_outline_lightbulb_orange_gradient_40dp)
        setHeaderTitle(R.string.stats_revamp_v2_intro_header_title)

        val contentBinding = StatsRevampV2FeaturesIntroContentViewBinding.inflate(layoutInflater)
        setContent(contentBinding.root)

        setPrimaryButtonText(R.string.stats_revamp_v2_intro_primary_button_text)
        setPrimaryButtonListener { viewModel.onPrimaryButtonClick() }

        setSecondaryButtonText(R.string.stats_revamp_v2_intro_secondary_button_text)
        setSecondaryButtonListener { viewModel.onSecondaryButtonClick() }
    }

    private fun setupObservers() {
        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is OpenStats -> {
                    activity?.let {
                        ActivityLauncher.viewBlogStats(it, action.site)
                    }
                }
                is DismissDialog -> {
                    dismiss()
                }
            }.exhaustive
        }
    }

    companion object {
        const val TAG = "STATS_NEW_FEATURES_INTRO_DIALOG_FRAGMENT"

        @JvmStatic
        fun newInstance(): StatsNewFeaturesIntroDialogFragment =
                StatsNewFeaturesIntroDialogFragment()
    }
}
