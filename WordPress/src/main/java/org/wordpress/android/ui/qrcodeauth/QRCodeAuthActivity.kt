package org.wordpress.android.ui.qrcodeauth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.QrcodeauthActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class QRCodeAuthActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(QrcodeauthActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(newIntent(context))
        }

        @JvmStatic
        fun newIntent(context: Context, uri: String? = null, fromDeeplink: Boolean = false): Intent {
            val intent = Intent(context, QRCodeAuthActivity::class.java).apply {
                putExtra(DEEP_LINK_URI_KEY, uri)
                putExtra(IS_DEEP_LINK_KEY, fromDeeplink)
            }
            return intent
        }

        const val IS_DEEP_LINK_KEY = "is_deep_link_key"
        const val DEEP_LINK_URI_KEY = "deep_link_uri_key"
    }
}
