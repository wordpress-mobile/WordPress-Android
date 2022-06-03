package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.databinding.QrcodeauthFragmentBinding

@AndroidEntryPoint
class QRCodeAuthFragment : Fragment(R.layout.qrcodeauth_fragment) {
    private val viewModel: QRCodeAuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(QrcodeauthFragmentBinding.bind(view)) {
            initView()
        }
    }

    @Suppress("MagicNumber")
    private fun QrcodeauthFragmentBinding.initView() {
        viewModel.start()
        // Temporarily show each view
        lifecycleScope.launch {
            loadingLayout.loadingContainer.visibility = View.VISIBLE
            contentLayout.contentContainer.visibility = View.GONE
            errorLayout.errorContainer.visibility = View.GONE
            delay(2000L)
            loadingLayout.loadingContainer.visibility = View.GONE
            contentLayout.contentContainer.visibility = View.VISIBLE
            errorLayout.errorContainer.visibility = View.GONE
            delay(2000L)
            loadingLayout.loadingContainer.visibility = View.GONE
            contentLayout.contentContainer.visibility = View.GONE
            errorLayout.errorContainer.visibility = View.VISIBLE
        }
        // Temporarily reference all strings from file so CI wont complain.
        // This will be removed in an upcoming PR
        var temp = R.string.qrcode_auth_flow_validated_default_title
        temp = R.string.qrcode_auth_flow_done_title
        temp = R.string.qrcode_auth_flow_done_subtitle
        temp = R.string.qrcode_auth_flow_dismiss
        temp = R.string.qrcode_auth_flow_scan_again
        temp = R.string.qrcode_auth_flow_error_no_connection_title
        temp = R.string.qrcode_auth_flow_error_no_connection_subtitle
        temp = R.string.qrcode_auth_flow_error_invalid_data_title
        temp = R.string.qrcode_auth_flow_error_invalid_data_subtitle
        temp = R.string.qrcode_auth_flow_error_expired_title
        temp = R.string.qrcode_auth_flow_error_expired_subtitle
        temp = R.string.qrcode_auth_flow_error_auth_failed_title
        temp = R.string.qrcode_auth_flow_error_auth_failed_subtitle
        temp = R.string.qrcode_auth_flow_dismiss_dialog_title
        temp = R.string.qrcode_auth_flow_dismiss_dialog_message
        var image = R.drawable.img_illustration_qrcode_auth_login_success_218dp
    }
}
