package org.wordpress.android.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.NotificationsPermissionBottomSheetBinding
import org.wordpress.android.util.WPPermissionUtils

class NotificationsPermissionBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.notifications_permission_bottom_sheet, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        with(NotificationsPermissionBottomSheetBinding.bind(view)) {
            val appName = getString(R.string.app_name)
            description.text = getString(R.string.notifications_permission_bottom_sheet_description_2, appName)

            primaryButton.setOnClickListener {
                WPPermissionUtils.showNotificationsSettings(requireActivity())
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "NOTIFICATIONS_PERMISSION_BOTTOM_SHEET_FRAGMENT"
    }
}
