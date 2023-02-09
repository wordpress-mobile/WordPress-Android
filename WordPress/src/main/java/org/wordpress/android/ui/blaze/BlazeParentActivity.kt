package org.wordpress.android.ui.blaze

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeOverlayFragment

@AndroidEntryPoint
class BlazeParentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_parent)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, BlazeOverlayFragment.newInstance())
                .commitNow()
        }
    }
}
