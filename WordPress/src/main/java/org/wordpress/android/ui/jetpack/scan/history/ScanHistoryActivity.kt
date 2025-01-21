package org.wordpress.android.ui.jetpack.scan.history

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.main.BaseAppCompatActivity

@AndroidEntryPoint
class ScanHistoryActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.scan_history_activity)
    }
}
