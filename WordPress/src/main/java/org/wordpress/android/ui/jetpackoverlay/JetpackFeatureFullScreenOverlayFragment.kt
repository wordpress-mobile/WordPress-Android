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
import org.wordpress.android.databinding.JetpackFeatureRemovalOverlayBinding
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.JETPACK_PACKAGE_NAME
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.DismissDialog
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.OpenPlayStore
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.main.WPMainNavigationView.PageType.MY_SITE
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.setVisible
import javax.inject.Inject

@AndroidEntryPoint
class JetpackFeatureFullScreenOverlayFragment : BottomSheetDialogFragment() {
    @Inject lateinit var activityLauncherWrapper: ActivityLauncherWrapper
    private val viewModel: JetpackFeatureFullScreenOverlayViewModel by viewModels()

    private var _binding: JetpackFeatureRemovalOverlayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = JetpackFeatureRemovalOverlayBinding.inflate(inflater, container, false)
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

    private fun getSiteScreen() =
            arguments?.getSerializable(OVERLAY_SCREEN_TYPE) as JetpackFeatureOverlayScreenType

    private fun rtlLayout(view: View): Boolean {
        val config: Configuration = resources.configuration
        view.layoutDirection = config.layoutDirection
        return view.layoutDirection == LAYOUT_DIRECTION_RTL
    }

    private fun JetpackFeatureRemovalOverlayBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            renderUiState(it)
        }

        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is OpenPlayStore -> {
                    dismiss()
                    activity?.let {
                        activityLauncherWrapper.openPlayStoreLink(it, JETPACK_PACKAGE_NAME)
                    }
                }
                is DismissDialog -> {
                    dismiss()
                }
            }.exhaustive
        }
    }

    private fun JetpackFeatureRemovalOverlayBinding.renderUiState(jetpackPoweredOverlayUIState: JetpackFeatureOverlayUIState) {
        updateVisibility(jetpackPoweredOverlayUIState.componentVisibility)
        updateContent(jetpackPoweredOverlayUIState.overlayContent)
        setClickListener(jetpackPoweredOverlayUIState.componentVisibility.secondaryButton)
    }

    private fun JetpackFeatureRemovalOverlayBinding.setClickListener(secondaryButtonVisible: Boolean) {
        primaryButton.setOnClickListener { viewModel.openJetpackAppDownloadLink() }
        if (secondaryButtonVisible) secondaryButton.setOnClickListener { viewModel.dismissBottomSheet() }
    }

    private fun JetpackFeatureRemovalOverlayBinding.updateVisibility(componentVisibility: JetpackFeatureOverlayComponentVisibility) {
        componentVisibility.let {
            illustrationView.setVisible(it.illustration)
            title.setVisible(it.title)
            caption.setVisible(it.caption)
            primaryButton.setVisible(it.primaryButton)
            secondaryButton.setVisible(it.secondaryButton)
        }
    }

    private fun JetpackFeatureRemovalOverlayBinding.updateContent(overlayContent: JetpackFeatureOverlayContent) {
        overlayContent.let {
            illustrationView.setAnimation(it.illustration)
            illustrationView.playAnimation()
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
        private const val OVERLAY_SCREEN_TYPE = "KEY_JETPACK_OVERLAY_SCREEN"

        @JvmStatic
        fun newInstance(
            jetpackFeatureOverlayScreenType: JetpackFeatureOverlayScreenType?
        ) = JetpackFeatureFullScreenOverlayFragment().apply {
            arguments = Bundle().apply {
                putSerializable(OVERLAY_SCREEN_TYPE, jetpackFeatureOverlayScreenType)
            }
        }
    }
}
