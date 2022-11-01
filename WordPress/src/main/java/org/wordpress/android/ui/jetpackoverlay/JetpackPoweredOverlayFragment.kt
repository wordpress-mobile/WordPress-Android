package org.wordpress.android.ui.jetpackoverlay

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.JetpackPoweredOverlayBinding
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.MY_SITE
import org.wordpress.android.util.extensions.setVisible
import javax.inject.Inject

@AndroidEntryPoint
class JetpackPoweredOverlayFragment : BottomSheetDialogFragment() {
    @Inject lateinit var activityLauncherWrapper: ActivityLauncherWrapper
    private val viewModel: JetpackPoweredFullScreenOverlayViewModel by viewModels()

    private var _binding: JetpackPoweredOverlayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = JetpackPoweredOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(getSiteScreen(), rtlLayout(view))
        binding.setupObservers()

        (dialog as? BottomSheetDialog)?.apply {
            setOnShowListener {
                val bottomSheet: FrameLayout = dialog?.findViewById(
                        com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                if (bottomSheet.layoutParams != null) {
                    showFullScreenBottomSheet(bottomSheet)
                }
                expandBottomSheet(bottomSheetBehavior)
            }
        }
    }

    private fun showFullScreenBottomSheet(bottomSheet: FrameLayout) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = Resources.getSystem().displayMetrics.heightPixels
        bottomSheet.layoutParams = layoutParams
    }

    private fun expandBottomSheet(bottomSheetBehavior: BottomSheetBehavior<FrameLayout>) {
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getSiteScreen() = arguments?.getSerializable(KEY_SITE_SCREEN) as? PageType ?: MY_SITE

    private fun rtlLayout(view: View): Boolean {
        val config: Configuration = resources.configuration
        view.layoutDirection = config.layoutDirection
        return view.layoutDirection == LAYOUT_DIRECTION_RTL
    }

    private fun JetpackPoweredOverlayBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            renderUiState(it)
        }
    }

    private fun JetpackPoweredOverlayBinding.renderUiState(jetpackPoweredOverlayUIState: JetpackFeatureOverlayUIState) {
        updateVisibility(jetpackPoweredOverlayUIState.componentVisibility)
        updateContent(jetpackPoweredOverlayUIState.overlayContent)
    }

    private fun JetpackPoweredOverlayBinding.updateVisibility(componentVisibility: JetpackFeatureOverlayComponentVisibility) {
        componentVisibility.let {
            illustrationView.setVisible(it.illustration)
            title.setVisible(it.title)
            caption.setVisible(it.caption)
            primaryButton.setVisible(it.primaryButton)
            secondaryButton.setVisible(it.secondaryButton)
        }
    }

    private fun JetpackPoweredOverlayBinding.updateContent(overlayContent: JetpackFeatureOverlayContent) {
        overlayContent.let {
            illustrationView.setAnimation(it.illustration)
            title.text = getString(it.title)
            caption.text = getString(it.caption)
            primaryButton.text = getString(it.primaryButtonText)
            secondaryButton.text = getString(it.secondaryButtonText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "JETPACK_POWERED_OVERLAY_FULL_SCREEN_FRAGMENT"
        private const val KEY_SITE_SCREEN = "KEY_SITE_SCREEN"

        @JvmStatic
        fun newInstance(
            pageType: PageType = MY_SITE
        ) = JetpackPoweredOverlayFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_SITE_SCREEN, pageType)
            }
        }
    }
}
