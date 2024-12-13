package org.wordpress.android.ui.main

import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import androidx.appcompat.app.AppCompatActivity
import android.R as AndroidR

@AndroidEntryPoint
class MeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.me_activity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
