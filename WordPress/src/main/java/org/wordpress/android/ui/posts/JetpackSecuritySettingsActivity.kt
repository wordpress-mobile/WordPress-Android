package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R.string
import org.wordpress.android.databinding.FragmentJetpackSecuritySettingsBinding

class JetpackSecuritySettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(FragmentJetpackSecuritySettingsBinding.inflate(layoutInflater)) {
            setContentView(root)
            setupToolbar()
        }

        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_OK, null)
            finish()
        }
    }

    private fun FragmentJetpackSecuritySettingsBinding.setupToolbar() {
        title = resources.getText(string.jetpack_security_setting_title)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemID = item.itemId
        if (itemID == android.R.id.home) {
            setResult(RESULT_OK, null)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val JETPACK_SECURITY_SETTINGS_REQUEST_CODE = 101
    }
}
