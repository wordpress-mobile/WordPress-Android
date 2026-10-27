package org.wordpress.android.ui.jetpack.scan

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ScanActivityBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import android.R as AndroidR

@AndroidEntryPoint
class ScanActivity : BaseAppCompatActivity() {
    private var binding: ScanActivityBinding? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        (supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? ScanFragment)?.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ScanActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this
            setSupportActionBar(toolbarMain)
        }
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        } else if (item.itemId == R.id.menu_scan_history) {
            // todo malinjir is it worth introducing a vm?
            ActivityLauncher.viewScanHistory(this, intent.getSerializableExtraCompat(WordPress.SITE))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scan_menu, menu)
        return true
    }

    companion object {
        const val REQUEST_SCAN_STATE = "request_scan_state"
        const val REQUEST_FIX_STATE = "request_fix_state"
    }
}
