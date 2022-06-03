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
    }
}
