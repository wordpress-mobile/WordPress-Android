package org.wordpress.android.ui.qrcodeauth

import android.content.Context
import android.content.Intent
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

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, QRCodeAuthActivity::class.java)
            context.startActivity(intent)
        }
    }
}
