package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.widgets.WPBottomSheetDialogFragment
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.prepublishing.PrepublishingPublishSettingsFragment
import javax.inject.Inject

class PrepublishingBottomSheetFragment : WPBottomSheetDialogFragment(),
        PrepublishingScreenClosedListener, PrepublishingActionClickedListener {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: PrepublishingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.WordPress_PrepublishingNudges_BottomSheetDialogTheme)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.post_prepublishing_bottom_sheet, container)
    }

    override fun onResume() {
        super.onResume()
        /**
         * The back button normally closes the bottom sheet so now instead of doing that it goes back to
         * the home screen with the actions and only if pressed again will it close the bottom sheet.
         */
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action != KeyEvent.ACTION_DOWN) {
                    true
                } else {
                    onBackClicked()
                    true
                }
            } else {
                false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel(savedInstanceState)
        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingViewModel::class.java)

        viewModel.navigationTarget.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { navigationState ->
                navigateToScreen(navigationState)
            }
        })

        viewModel.dismissBottomSheet.observe(this, Observer { event ->
            event.applyIfNotHandled { dismiss() }
        })

        val prepublishingScreenState = savedInstanceState?.getParcelable<PrepublishingScreen>(KEY_SCREEN_STATE)
        val site = arguments?.getSerializable(SITE) as SiteModel

        viewModel.start(site, prepublishingScreenState)
    }

    private fun navigateToScreen(navigationTarget: PrepublishingNavigationTarget) {
        val (fragment, tag) = when (navigationTarget.targetScreen) {
            HOME -> Pair(
                    PrepublishingHomeFragment.newInstance(),
                    PrepublishingHomeFragment.TAG
            )
            PrepublishingScreen.PUBLISH -> Pair(
                    PrepublishingPublishSettingsFragment.newInstance(),
                    PrepublishingPublishSettingsFragment.TAG
            )
            PrepublishingScreen.VISIBILITY -> TODO()
            PrepublishingScreen.TAGS -> Pair(
                    PrepublishingTagsFragment.newInstance(navigationTarget.site), PrepublishingTagsFragment.TAG
            )
        }

        fadeInFragment(fragment, tag)
    }

    private fun fadeInFragment(fragment: Fragment, tag: String) {
        childFragmentManager.let { fragmentManager ->
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentManager.findFragmentById(R.id.prepublishing_content_fragment)?.run {
                fragmentTransaction.addToBackStack(null).setCustomAnimations(
                        R.anim.prepublishing_fragment_fade_in, R.anim.prepublishing_fragment_fade_out,
                        R.anim.prepublishing_fragment_fade_in, R.anim.prepublishing_fragment_fade_out
                )
            }
            fragmentTransaction.replace(R.id.prepublishing_content_fragment, fragment, tag)
            fragmentTransaction.commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    companion object {
        const val TAG = "prepublishing_bottom_sheet_fragment_tag"
        const val SITE = "prepublishing_bottom_sheet_site_model"

        @JvmStatic
        fun newInstance(@NonNull site: SiteModel) = PrepublishingBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putSerializable(SITE, site)
            }
        }
    }

    override fun onCloseClicked() {
        viewModel.onCloseClicked()
    }

    override fun onBackClicked() {
        viewModel.onBackClicked()
    }

    override fun onActionClicked(actionType: ActionType) {
        viewModel.onActionClicked(actionType)
    }
}
