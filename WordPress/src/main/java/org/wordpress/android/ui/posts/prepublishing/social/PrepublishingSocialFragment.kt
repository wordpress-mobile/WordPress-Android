package org.wordpress.android.ui.posts.prepublishing.social

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PrepublishingSocialFragmentBinding
import org.wordpress.android.databinding.PrepublishingToolbarBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.withBottomSheetElevation
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.prepublishing.PrepublishingViewModel
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingSocialViewModelProvider
import org.wordpress.android.ui.posts.prepublishing.social.compose.PrepublishingSocialScreen
import javax.inject.Inject

class PrepublishingSocialFragment : Fragment(R.layout.prepublishing_social_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var parentViewModel: PrepublishingViewModel
    private lateinit var socialViewModel: EditorJetpackSocialViewModel

    private var binding: PrepublishingSocialFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PrepublishingSocialFragmentBinding.bind(view)) {
            binding = this
            includePrepublishingToolbar.init()
        }
        setupViewModels()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun PrepublishingToolbarBinding.init() {
        toolbarTitle.text = getString(R.string.prepublishing_nudges_toolbar_title_social)
        backButton.setOnClickListener { parentViewModel.onBackClicked() }
    }

    private fun setupViewModels() {
        parentViewModel = ViewModelProvider(
            requireParentFragment(),
            viewModelFactory
        )[PrepublishingViewModel::class.java]

        socialViewModel = (parentFragment as PrepublishingSocialViewModelProvider).getEditorJetpackSocialViewModel()
    }

    private fun setupObservers() {
        socialViewModel.jetpackSocialUiState.observe(viewLifecycleOwner) { state ->
            if (state is JetpackSocialUiState.Loaded) {
                binding?.apply {
                    prepublishingSocialComposeView.setContent {
                        AppThemeM2 {
                            PrepublishingSocialScreen(
                                state = state,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colors.surface.withBottomSheetElevation()),
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "prepublishing_social_fragment_tag"

        @JvmStatic
        fun newInstance() = PrepublishingSocialFragment()
    }
}
