package org.wordpress.android.ui.quickstart

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface

class QuickStartActivity : AppCompatActivity(), BasicFragmentDialog.BasicDialogPositiveClickInterface {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_start_activity)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onPositiveClicked(instanceTag: String) {
        supportFragmentManager.findFragmentById(R.id.quick_start_fragment).let {
            (it as BasicDialogPositiveClickInterface).onPositiveClicked(instanceTag)
        }
    }
}
