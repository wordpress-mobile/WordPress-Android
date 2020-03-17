package org.wordpress.android.ui.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.prefs.AppSettingsFragment.LANGUAGE_CHANGED
import org.wordpress.android.util.LocaleManager

class MeActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        super.applyOverrideConfiguration(LocaleManager.updatedConfigLocale(baseContext, overrideConfiguration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.me_activity)

        setSupportActionBar(toolbar_main)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            val activityInfo = packageManager.getActivityInfo(
                    componentName,
                    PackageManager.GET_META_DATA
            )
            it.setTitle(activityInfo.labelRes)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.APP_SETTINGS -> {
                if (resultCode == LANGUAGE_CHANGED) {
                    // Refresh the app
                    val refresh = Intent(this, this.javaClass)
                    startActivity(refresh)
                    setResult(LANGUAGE_CHANGED)
                    finish()
                }
            }
        }
    }
}
