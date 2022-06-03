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
            loadingLayout.container.visibility = View.VISIBLE
            contentLayout.container.visibility = View.GONE
            errorLayout.container.visibility = View.GONE
            delay(2000L)
            loadingLayout.container.visibility = View.GONE
            contentLayout.container.visibility = View.VISIBLE
            errorLayout.container.visibility = View.GONE
            delay(2000L)
            loadingLayout.container.visibility = View.GONE
            contentLayout.container.visibility = View.GONE
            errorLayout.container.visibility = View.VISIBLE
        }
    }
}
