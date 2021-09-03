package org.wordpress.android.ui.jetpack.scan

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ScanActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher

class ScanActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? ScanFragment)?.let {
            it.onNewIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ScanActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbarMain)
        }
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.menu_scan_history) {
            // todo malinjir is it worth introducing a vm?
            ActivityLauncher.viewScanHistory(this, intent.getSerializableExtra(WordPress.SITE) as SiteModel)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scan_menu, menu)
        return true
    }

    companion object {
        const val REQUEST_SCAN_STATE = "request_scan_state"
        const val REQUEST_FIX_STATE = "request_fix_state"
    }
}
