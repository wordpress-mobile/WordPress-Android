package org.wordpress.android.ui.posts

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.extensions.getSerializableCompat
import javax.inject.Inject

/**
 * One-time announcement bottom sheet for the upcoming GutenbergKit editor. Show/defer/activate
 * logic lives in [GutenbergKitAnnouncementController]; this fragment hosts a Compose layout and
 * forwards button taps.
 */
@AndroidEntryPoint
class GutenbergKitAnnouncementBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var controller: GutenbergKitAnnouncementController

    private var decisionRecorded = false

    // The default `WordPress.BottomSheetDialogTheme` sets `fitsSystemWindows=true`, which adds
    // the status-bar inset as top padding to the sheet container. The `NonTranslucent` variant
    // turns that off — matches what other Compose bottom sheets (e.g. ReaderSubscriptionSettings)
    // already do.
    override fun getTheme(): Int = R.style.WordPress_BottomSheetDialogTheme_NonTranslucent

    private val site: SiteModel
        get() = requireNotNull(
            requireArguments().getSerializableCompat<SiteModel>(WordPress.SITE)
        ) { "GutenbergKitAnnouncementBottomSheetFragment requires a SiteModel argument" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM3 {
                GutenbergKitAnnouncementScreen(
                    onActivate = {
                        controller.onActivate(site)
                        decisionRecorded = true
                        dismiss()
                    },
                    onMaybeLater = {
                        controller.onMaybeLater(site)
                        decisionRecorded = true
                        dismiss()
                    },
                    onLearnMore = {
                        WPWebViewActivity.openURL(
                            requireContext(),
                            getString(R.string.gutenberg_kit_learn_more_url),
                        )
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Defer the expand to the show callback so the sheet slides up from offscreen instead of
        // starting at its final position (which skips the slide-in animation entirely).
        (dialog as? BottomSheetDialog)?.apply {
            behavior.skipCollapsed = true
            setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Swipe / back / tap-outside without a button tap is treated as an implicit "Maybe later"
        // so the sheet doesn't re-prompt on the next My Site resume. Config changes don't count.
        if (decisionRecorded) return
        if (activity?.isChangingConfigurations == true) return
        controller.onMaybeLater(site)
    }

    companion object {
        const val TAG = "GutenbergKitAnnouncementBottomSheetFragment"

        fun newInstance(site: SiteModel): GutenbergKitAnnouncementBottomSheetFragment =
            GutenbergKitAnnouncementBottomSheetFragment().apply {
                arguments = Bundle().apply { putSerializable(WordPress.SITE, site) }
            }
    }
}
