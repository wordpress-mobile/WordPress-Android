package org.wordpress.android.ui.sitecreation.verticals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteCreationIntentsFragmentBinding
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
        with(binding) {
            this@SiteCreationIntentsFragment.binding = binding
            setupUi()
            setupViewModel()
            setupActionListeners()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun SiteCreationIntentsFragmentBinding.setupUi() {
        siteCreationIntentsTitlebar.title.visibility = if (isPhoneLandscape()) View.VISIBLE else View.INVISIBLE
        siteCreationHeaderItem.title.setText(R.string.new_site_creation_intents_header_title)
        siteCreationHeaderItem.subtitle.setText(R.string.new_site_creation_intents_header_subtitle)
    }

    private fun SiteCreationIntentsFragmentBinding.setupViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            setHeaderVisibility(uiState.isHeaderVisible)
        }

        viewModel.start()
    }

    private fun SiteCreationIntentsFragmentBinding.setHeaderVisibility(visible: Boolean) {
        uiHelper.fadeInfadeOutViews(
                siteCreationIntentsTitlebar.title,
                siteCreationHeaderItem.title,
                visible
        )
    }

    private fun SiteCreationIntentsFragmentBinding.setupActionListeners() {
        siteCreationIntentsTitlebar.skipButton.setOnClickListener { viewModel.onSkipPressed() }
        siteCreationIntentsTitlebar.backButton.setOnClickListener { viewModel.onBackPressed() }
        setScrollListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun SiteCreationIntentsFragmentBinding.setScrollListener() {
        val scrollThreshold = resources.getDimension(R.dimen.picker_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
        viewModel.onAppBarOffsetChanged(0, scrollThreshold)
    }

    private fun isPhoneLandscape() = displayUtils.isLandscapeBySize() && !displayUtils.isTablet()

    companion object {
        const val TAG = "site_creation_intents_fragment_tag"
    }
}
