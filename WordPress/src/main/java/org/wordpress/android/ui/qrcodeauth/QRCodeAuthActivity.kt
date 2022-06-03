package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.QrcodeauthActivityBinding

@AndroidEntryPoint
class QRCodeAuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(QrcodeauthActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
        }
    }
}
