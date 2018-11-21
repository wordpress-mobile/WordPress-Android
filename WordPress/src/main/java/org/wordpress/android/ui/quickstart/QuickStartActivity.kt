package org.wordpress.android.ui.quickstart

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.wordpress.android.R

class QuickStartActivity : AppCompatActivity() {
    companion object {
        const val ARG_QUICK_START_TASK = "quick_start_task"
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_start_activity)
    }

    // TODO to be used with future branches
    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
