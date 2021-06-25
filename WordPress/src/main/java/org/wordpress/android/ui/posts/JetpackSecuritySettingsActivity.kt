package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.R.string

class JetpackSecuritySettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.fragment_jetpack_security_settings)
        setupToolbar()
    }

    private fun setupToolbar() {
        title = resources.getText(string.jetpack_security_setting_title)
        val toolbar = findViewById<Toolbar>(id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            val actionBar = supportActionBar
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(true)
                actionBar.setDisplayHomeAsUpEnabled(true)
            }
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

    override fun onBackPressed() {
        setResult(RESULT_OK, null)
        finish()
    }

    companion object {
        const val JETPACK_SECURITY_SETTINGS_REQUEST_CODE = 101
    }
}
