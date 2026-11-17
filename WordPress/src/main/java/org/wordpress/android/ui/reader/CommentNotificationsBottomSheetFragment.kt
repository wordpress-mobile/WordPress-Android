package org.wordpress.android.ui.reader

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel
import org.wordpress.android.ui.reader.views.compose.ReaderSheetHandle
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

    // kept in a field so the view model's status updates and the restored state reach the composition
    private val isReceivingPushNotifications: MutableState<Boolean> = mutableStateOf(false)

    // this avoids a weird animation when opening this dialog from activity that is using
    // @style/WordPress.NoActionBar.TranslucentStatus
    override fun getTheme(): Int {
        return R.style.WordPress_BottomSheetDialogTheme_NonTranslucent
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isReceivingPushNotifications.value = savedInstanceState?.getBoolean(STATE_IS_RECEIVING_PUSH_NOTIFICATIONS)
            ?: requireArguments().getBoolean(ARG_IS_RECEIVING_PUSH_NOTIFICATIONS)
        initViewModel(requireArguments().getBoolean(ARG_IS_ATTACHED_TO_FRAGMENT))

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var isChecked by isReceivingPushNotifications
                AppThemeM3 {
                    CommentNotificationsSheet(
                        isReceivingPushNotifications = isChecked,
                        onPushNotificationsToggled = { checked ->
                            isChecked = checked
                            viewModel.onChangePushNotificationsRequest(checked)
                        },
                        onUnfollowClick = viewModel::onUnfollowTapped
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers(view)

        (dialog as? BottomSheetDialog)?.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_IS_RECEIVING_PUSH_NOTIFICATIONS, isReceivingPushNotifications.value)
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

    private fun initObservers(view: View) {
        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, { messageHolder ->
            if (!isAdded) return@observeEvent

            WPSnackbar.make(
                view,
                uiHelpers.getTextOfUiString(contextProvider.getContext(), messageHolder.message),
                Snackbar.LENGTH_LONG
            ).show()
        })

        viewModel.pushNotificationsStatusUpdate.observeEvent(viewLifecycleOwner, { isReceiving ->
            isReceivingPushNotifications.value = isReceiving
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    companion object {
        private const val ARG_IS_RECEIVING_PUSH_NOTIFICATIONS = "ARG_IS_RECEIVING_PUSH_NOTIFICATIONS"
        private const val ARG_IS_ATTACHED_TO_FRAGMENT = "ARG_IS_ATTACHED_TO_FRAGMENT"
        private const val STATE_IS_RECEIVING_PUSH_NOTIFICATIONS = "STATE_IS_RECEIVING_PUSH_NOTIFICATIONS"

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

@Composable
private fun CommentNotificationsSheet(
    isReceivingPushNotifications: Boolean,
    onPushNotificationsToggled: (Boolean) -> Unit,
    onUnfollowClick: () -> Unit
) {
    val sheetDescription = stringResource(R.string.reader_follow_comments_bottom_sheet_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = sheetDescription }
            .padding(start = Margin.ExtraLarge.value, end = Margin.ExtraLarge.value, bottom = Margin.ExtraLarge.value)
    ) {
        ReaderSheetHandle()
        Text(
            text = stringResource(R.string.reader_follow_comments_message),
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Margin.Large.value)
                .toggleable(
                    value = isReceivingPushNotifications,
                    role = Role.Switch,
                    onValueChange = onPushNotificationsToggled
                )
                .padding(vertical = Margin.Large.value)
        ) {
            Text(
                text = stringResource(R.string.reader_follow_comments_enable_push_notifications),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = isReceivingPushNotifications, onCheckedChange = null)
        }
        OutlinedButton(
            onClick = onUnfollowClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Margin.Large.value)
        ) {
            Text(stringResource(R.string.reader_follow_comments_unfollow_conversation))
        }
    }
}
