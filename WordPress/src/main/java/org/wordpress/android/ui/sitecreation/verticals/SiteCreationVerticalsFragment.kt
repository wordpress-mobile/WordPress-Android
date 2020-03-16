package org.wordpress.android.ui.sitecreation.verticals

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.OnSkipClickedListener
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject
import kotlin.properties.Delegates

class SiteCreationVerticalsFragment : SiteCreationBaseFormFragment() {
    private lateinit var nonNullActivity: FragmentActivity
    private var segmentId by Delegates.notNull<Long>()
    private lateinit var viewModel: SiteCreationVerticalsViewModel

    private lateinit var contentLayout: ViewGroup
    private lateinit var skipButton: Button

    private lateinit var verticalsScreenListener: VerticalsScreenListener
    private lateinit var helpClickedListener: OnHelpClickedListener
    private lateinit var skipClickedListener: OnSkipClickedListener

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is VerticalsScreenListener) {
            throw IllegalStateException("Parent activity must implement VerticalsScreenListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
        if (context !is OnSkipClickedListener) {
            throw IllegalStateException("Parent activity must implement OnSkipClickedListener.")
        }
        verticalsScreenListener = context
        helpClickedListener = context
        skipClickedListener = context
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_verticals_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        contentLayout = rootView.findViewById(R.id.content_layout)

        initSkipButton(rootView)
        initViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = activity!!
        (nonNullActivity.application as WordPress).component().inject(this)
        segmentId = arguments?.getLong(EXTRA_SEGMENT_ID, -1L) ?: -1L
        if (segmentId == -1L) {
            throw IllegalStateException("SegmentId is required.")
        }
    }

    private fun initSkipButton(rootView: ViewGroup) {
        skipButton = rootView.findViewById(R.id.btn_skip)
        skipButton.setOnClickListener { viewModel.onSkipStepBtnClicked() }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SiteCreationVerticalsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                uiHelpers.updateVisibility(contentLayout, uiState.contentLayoutVisibility)

                when (uiState) {
                    is VerticalsContentUiState -> updateContentLayout(uiState)
                }
            }
        })

        viewModel.verticalSelected.observe(this, Observer { verticalId ->
            verticalId?.let { verticalsScreenListener.onVerticalSelected(verticalId) }
        })
        viewModel.skipBtnClicked.observe(this, Observer { skipClickedListener.onSkipClicked() })
        viewModel.onHelpClicked.observe(this, Observer {
            helpClickedListener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_VERTICALS)
        })
        viewModel.start(segmentId)
    }

    private fun updateContentLayout(uiState: VerticalsContentUiState) {
        uiHelpers.updateVisibility(skipButton, uiState.showSkipButton)
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
    }

    companion object {
        const val TAG = "site_creation_verticals_fragment_tag"
        private const val EXTRA_SEGMENT_ID = "extra_segment_id"

        fun newInstance(screenTitle: String, segmentId: Long): SiteCreationVerticalsFragment {
            val fragment = SiteCreationVerticalsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            bundle.putLong(EXTRA_SEGMENT_ID, segmentId)
            fragment.arguments = bundle
            return fragment
        }
    }
}
