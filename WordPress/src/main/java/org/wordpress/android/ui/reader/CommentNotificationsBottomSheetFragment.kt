package org.wordpress.android.ui.reader

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.CommentNotificationsBottomSheetBinding
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class CommentNotificationsBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var contextProvider: ContextProvider
    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: ConversationNotificationsViewModel

    // this avoids a weird animation when opening this dialog from activity that is using
    // @style/WordPress.NoActionBar.TranslucentStatus
    override fun getTheme(): Int {
        return R.style.WordPress_BottomSheetDialogTheme_NonTranslucent
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.comment_notifications_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(CommentNotificationsBottomSheetBinding.bind(view)) {
            val isReceivingPushNotifications = requireArguments().getBoolean(ARG_IS_RECEIVING_PUSH_NOTIFICATIONS)
            val isAttachedToFragment = requireArguments().getBoolean(ARG_IS_ATTACHED_TO_FRAGMENT)

            initViewModel(isAttachedToFragment)

            if (savedInstanceState == null) {
                enablePushNotifications.isChecked = isReceivingPushNotifications
            }

            unfollowConversation.setOnClickListener {
                viewModel.onUnfollowTapped()
            }

            enablePushNotifications.setOnClickListener {
                viewModel.onChangePushNotificationsRequest((it as SwitchCompat).isChecked)
            }

            initObservers()
        }

        (dialog as? BottomSheetDialog)?.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    private fun initViewModel(isAttachedToFragment: Boolean) {
        viewModel = ViewModelProvider(
            if (isAttachedToFragment) {
                parentFragment as ViewModelStoreOwner
            } else {
                requireActivity()
            },
            viewModelFactory
        ).get(ConversationNotificationsViewModel::class.java)
    }

    private fun CommentNotificationsBottomSheetBinding.initObservers() {
        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, { messageHolder ->
            if (!isAdded) return@observeEvent

            WPSnackbar.make(
                coordinator,
                uiHelpers.getTextOfUiString(contextProvider.getContext(), messageHolder.message),
                Snackbar.LENGTH_LONG
            ).show()
        })

        viewModel.pushNotificationsStatusUpdate.observeEvent(viewLifecycleOwner, { isReceivingPushNotifications ->
            enablePushNotifications.isChecked = isReceivingPushNotifications
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        private const val ARG_IS_RECEIVING_PUSH_NOTIFICATIONS = "ARG_IS_RECEIVING_PUSH_NOTIFICATIONS"
        private const val ARG_IS_ATTACHED_TO_FRAGMENT = "ARG_IS_ATTACHED_TO_FRAGMENT"

        @JvmStatic
        fun newInstance(
            isReceivingPushNotifications: Boolean,
            isAttachedToFragment: Boolean
        ): CommentNotificationsBottomSheetFragment {
            val fragment = CommentNotificationsBottomSheetFragment()
            val bundle = Bundle()
            bundle.putBoolean(ARG_IS_RECEIVING_PUSH_NOTIFICATIONS, isReceivingPushNotifications)
            bundle.putBoolean(ARG_IS_ATTACHED_TO_FRAGMENT, isAttachedToFragment)
            fragment.arguments = bundle
            return fragment
        }
    }
}
