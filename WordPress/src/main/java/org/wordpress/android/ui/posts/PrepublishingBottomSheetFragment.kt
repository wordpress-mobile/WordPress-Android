package org.wordpress.android.ui.posts

import android.os.Bundle
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
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PrepublishingScreenState.HomeState
import org.wordpress.android.ui.posts.PrepublishingScreenState.TagsState
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import javax.inject.Inject

class PrepublishingBottomSheetFragment : WPBottomSheetDialogFragment(),
        TagsSelectedListener,
        PrepublishingScreenClosedListener {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var prepublishingViewModel: PrepublishingViewModel
    private lateinit var prepublishingActionsViewModel: PrepublishingActionsViewModel

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModels()
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

    private fun initViewModels() {
        prepublishingViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingViewModel::class.java)

        prepublishingActionsViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingActionsViewModel::class.java)

        prepublishingActionsViewModel.onActionClicked.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { actionType ->
                prepublishingViewModel.onActionClicked(actionType)
            }
        })

        prepublishingViewModel.navigationTarget.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { navigationState ->
                navigateToScreen(navigationState)
            }
        })

        val prepublishingActionState = arguments?.getParcelable<PrepublishingScreenState>(KEY_TAGS_ACTION_STATE)
        val site = arguments?.getSerializable(SITE) as SiteModel

        prepublishingViewModel.start(getPostRepository(), site, prepublishingActionState)
    }

    private fun navigateToScreen(navigationTarget: PrepublishingNavigationTarget) {
        val (fragment, tag) = when (navigationTarget.targetScreen) {
            HOME -> Pair(
                    PrepublishingActionsFragment.newInstance((navigationTarget.screenState as HomeState)),
                    PrepublishingActionsFragment.TAG
            )
            PrepublishingScreen.PUBLISH -> TODO()
            PrepublishingScreen.VISIBILITY -> TODO()
            PrepublishingScreen.TAGS -> Pair(
                    PrepublishingTagsFragment.newInstance(
                            navigationTarget.site,
                            (navigationTarget.screenState as? TagsState)?.tags
                    ),
                    PrepublishingTagsFragment.TAG
            )
        }

        slideInFragment(fragment, tag, tag != PrepublishingActionsFragment.TAG)
    }

    private fun slideInFragment(fragment: Fragment, tag: String, slideIn: Boolean) {
        childFragmentManager.let { fragmentManager ->
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentManager.findFragmentById(R.id.prepublishing_content_fragment)?.run {
                if (slideIn) {
                    fragmentTransaction.addToBackStack(null).setCustomAnimations(
                            R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                            R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
                    )
                } else {
                    fragmentTransaction.addToBackStack(null).setCustomAnimations(
                            R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right,
                            R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left
                    )
                }
            }
            fragmentTransaction.replace(R.id.prepublishing_content_fragment, fragment, tag)
            fragmentTransaction.commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        prepublishingViewModel.writeToBundle(outState)
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

    private fun getPostRepository(): EditPostRepository? {
        return getEditPostActivityHook()?.editPostRepository
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null

        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    override fun onTagsSelected(selectedTags: String) {
        prepublishingViewModel.updateTagsState(selectedTags)
    }

    override fun onCloseClicked() {
        dismiss()
    }

    override fun onBackClicked() {
        prepublishingViewModel.onCloseClicked()
    }
}
