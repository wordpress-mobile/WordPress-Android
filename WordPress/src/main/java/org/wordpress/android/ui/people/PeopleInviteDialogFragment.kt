package org.wordpress.android.ui.people

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.people.PeopleInviteDialogFragment.DialogMode.DISABLE_INVITE_LINKS_CONFIRMATION
import org.wordpress.android.ui.people.PeopleInviteDialogFragment.DialogMode.INVITE_LINKS_ROLE_SELECTION
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

/**
 * This dialog is used in PeopleInviteFragment in the following scenarios
 * based on DialogMode
 *
 * When DialogMode is INVITE_LINKS_ROLE_SELECTION: the dialog shows a simple dialog
 * listing the available roles for sites to choose from
 *
 * When DialogMode is DISABLE_INVITE_LINKS_CONFIRMATION: the dialog shows a confirmation
 * dialog asking to confirm to disable or not the currently available invite links
 */

class PeopleInviteDialogFragment : DialogFragment() {
    @Inject
    lateinit var contextProvider: ContextProvider
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: PeopleInviteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(
            targetFragment as ViewModelStoreOwner, viewModelFactory
        ).get(PeopleInviteViewModel::class.java)

        val dialogMode = arguments?.getSerializable(ARG_DIALOG_MODE) as? DialogMode
        val roles = arguments?.getStringArray(ARG_ROLES)
        val builder = MaterialAlertDialogBuilder(requireActivity())

        dialogMode?.let { mode ->
            builder.apply {
                setTitle(dialogMode.title)

                when (mode) {
                    INVITE_LINKS_ROLE_SELECTION -> {
                        setItems(roles) { _, which ->
                            if (!isAdded) return@setItems

                            viewModel.onLinksRoleSelected(roles?.get(which) ?: "")
                        }
                    }
                    DISABLE_INVITE_LINKS_CONFIRMATION -> {
                        setMessage(mode.message)
                        setNegativeButton(mode.negativeButtonText, null)
                        setPositiveButton(mode.positiveButtonText) { _, _ ->
                            if (!isAdded) return@setPositiveButton
                            viewModel.onDisableLinksButtonClicked()
                        }
                    }
                }
            }
        }

        return builder.create()
    }

    companion object {
        private const val ARG_DIALOG_MODE = "dialog_mode"
        private const val ARG_ROLES = "roles"

        @JvmStatic
        @JvmOverloads
        @Suppress("DEPRECATION")
        fun newInstance(
            fragment: Fragment,
            dialogMode: DialogMode,
            roles: Array<String> = arrayOf()
        ): PeopleInviteDialogFragment {
            return PeopleInviteDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DIALOG_MODE, dialogMode)
                    putStringArray(ARG_ROLES, roles)
                }
                setTargetFragment(fragment, 0)
            }
        }
    }

    enum class DialogMode(
        val title: Int,
        val message: Int = 0,
        val negativeButtonText: Int = 0,
        val positiveButtonText: Int = 0
    ) {
        INVITE_LINKS_ROLE_SELECTION(
            R.string.role
        ),
        DISABLE_INVITE_LINKS_CONFIRMATION(
            R.string.invite_links_disable_dialog_title,
            R.string.invite_links_disable_dialog_message,
            R.string.cancel,
            R.string.disable
        )
    }
}
