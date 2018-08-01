package org.wordpress.android.ui.pages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.pages_fragment.*
import org.wordpress.android.R
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.posts.EditPostActivity

class PagesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pages_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
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
        if (requestCode == RequestCodes.EDIT_POST && resultCode == Activity.RESULT_OK && data != null) {
            val pageId = data.getLongExtra(EditPostActivity.EXTRA_POST_REMOTE_ID, 0)
            onPageEditFinished(pageId)
        }
    }

    private fun onPageEditFinished(pageId: Long) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PagesFragment) {
            fragment.onPageEditFinished(pageId)
        }
    }
}
