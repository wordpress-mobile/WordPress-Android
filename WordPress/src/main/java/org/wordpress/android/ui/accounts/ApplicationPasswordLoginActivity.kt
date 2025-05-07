package org.wordpress.android.ui.accounts

import android.content.Intent
import android.os.Bundle
import android.util.Log
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.main.WPMainActivity

class ApplicationPasswordLoginActivity: BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("WP_RS", "Intent: " + intent.dataString)

        val intent = Intent(this, WPMainActivity::class.java)
        intent.setFlags(
            (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        startActivity(intent)
        finish()
    }
}
