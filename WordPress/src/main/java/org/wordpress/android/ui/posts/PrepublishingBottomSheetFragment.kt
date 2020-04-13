package org.wordpress.android.ui.posts

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.apache.commons.lang3.NotImplementedException
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.login.widgets.WPBottomSheetDialogFragment
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.VISIBILITY
import javax.inject.Inject

class PrepublishingBottomSheetFragment : WPBottomSheetDialogFragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var prepublishingViewModel: PrepublishingViewModel
    private lateinit var prepublishingActionsViewModel: PrepublishingActionsViewModel

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
                navigateToAction(actionType)
            }
        })
    }

    // Create a sealed class to hold the state for all these actions.
    private fun navigateToAction(actionType: ActionType) {
        val fragment = when (actionType) {
            TAGS -> PostSettingsTagsFragment.newInstance(null, null)
            else -> throw NotImplementedError()
        }
        }
        slideInFragment(fragment)
    }

    private fun slideInFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            // add to back stack and animate all screen except of the first one
            fragmentTransaction.addToBackStack(null).setCustomAnimations(
                    R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
            )
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)


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
