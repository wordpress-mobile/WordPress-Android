package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
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

    private fun QrcodeauthFragmentBinding.initView() {
        qrcodeAuthMessage.text = getString(R.string.qrcode_auth_flow)
        viewModel.start()
    }
}
