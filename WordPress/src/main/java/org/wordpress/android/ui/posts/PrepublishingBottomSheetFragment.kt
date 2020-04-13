package org.wordpress.android.ui.posts

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import org.wordpress.android.ui.posts.ActionState.TagsActionState
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import javax.inject.Inject

class PrepublishingBottomSheetFragment : WPBottomSheetDialogFragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var prepublishingViewModel: PrepublishingViewModel
    private lateinit var prepublishingActionsViewModel: PrepublishingActionsViewModel

    private lateinit var site: SiteModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        prepublishingActionsViewModel.prepublishingActionType.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { actionType ->
                prepublishingViewModel.updateCurrentActionTypeState(actionType)
            }
        })

        prepublishingViewModel.currentActionsState.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { actionState ->
                navigateToScreen(actionState)
            }
        })

        var actionsState = arguments?.getParcelable<ActionsState>(KEY_TAGS_ACTION_STATE)
        if (actionsState == null) {
            actionsState = ActionsState(tagsActionState = TagsActionState(tags = null))
        }
        prepublishingViewModel.start(actionsState)
    }

    private fun navigateToScreen(actionState: ActionsState) {
        val result = when (actionState.currentScreen) {
            HOME -> Pair(PrepublishingActionsFragment.newInstance(), PrepublishingActionsFragment.TAG)
            PrepublishingScreen.PUBLISH -> TODO()
            PrepublishingScreen.VISIBILITY -> TODO()
            PrepublishingScreen.TAGS -> Pair(
                    PostSettingsTagsFragment.newInstance(site, actionState.tagsActionState.tags),
                    PostSettingsTagsFragment.TAG
            )
        }

        slideInFragment(result.first, result.second)
    }

    private fun slideInFragment(fragment: Fragment, tag: String) {
        childFragmentManager.let { fragmentManager ->
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentManager.findFragmentById(R.id.prepublishing_content_fragment)?.run {
                fragmentTransaction.addToBackStack(null).setCustomAnimations(
                        R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                        R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
                )
            }
            fragmentTransaction.replace(R.id.prepublishing_content_fragment, fragment, tag)
            fragmentTransaction.commit()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        removeContentFragmentBeforeDismissal()
    }

    private fun removeContentFragmentBeforeDismissal() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            fragmentManager.findFragmentById(R.id.prepublishing_content_fragment)?.also { fragment ->
                fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
            }
        }
    }

    companion object {
        const val TAG = "prepublishing_bottom_sheet_fragment_tag"

        @JvmStatic
        fun newInstance(): PrepublishingBottomSheetFragment {
            return PrepublishingBottomSheetFragment()
        }
    }
}
