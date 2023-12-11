package org.wordpress.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.WPMainActivity
import org.wordpress.android.util.MissingSplitsUtils.isMissingSplits
import org.wordpress.android.util.ProfilingUtils
import org.wordpress.android.util.ToastUtils

@SuppressLint("CustomSplashScreen")
class WPLaunchActivity : LocaleAwareActivity() {
    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup - note that it's defined in the
     * manifest to have no UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupClarity()
        if (isMissingSplits(this)) {
            // There are missing splits. Display a warning message.
            showMissingSplitsDialog()
            return
        }
        ProfilingUtils.split("WPLaunchActivity.onCreate")
        launchWPMainActivity()
    }

    private fun setupClarity() {
        if (BuildConfig.IS_JETPACK_APP) {
            val config = ClarityConfig(projectId = BuildConfig.CLARITY_ID)
            Clarity.initialize(applicationContext, config)
        }
    }

    private fun showMissingSplitsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.missing_splits_dialog_title)
            .setMessage(R.string.missing_splits_dialog_message)
            .setNegativeButton(R.string.missing_splits_dialog_button, null)
            .setOnDismissListener { finish() }
            .show()
    }

    private fun launchWPMainActivity() {
        if (!WordPress.isWpDBInitialized) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG)
            finish()
            return
        }
        val intent = Intent(this, WPMainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = getIntent().action
        intent.data = getIntent().data
        startActivity(intent)
        finish()
    }
}
