package org.wordpress.android.imageeditor

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentTransaction

import org.wordpress.android.imageeditor.fragments.MainImageFragment

class EditImageActivity : AppCompatActivity() {
    // initial media item to show, based either on ID or URI
    private var mediaId: Int = 0
    private var contentUri: String? = null
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.edit_image_activity)

        if (savedInstanceState != null) {
            mediaId = savedInstanceState.getInt(MainImageFragment.ARG_MEDIA_ID)
            contentUri = savedInstanceState.getString(MainImageFragment.ARG_MEDIA_CONTENT_URI)
        } else {
            mediaId = intent.getIntExtra(MainImageFragment.ARG_MEDIA_ID, 0)
            contentUri = intent.getStringExtra(MainImageFragment.ARG_MEDIA_CONTENT_URI)
        }

        if (TextUtils.isEmpty(contentUri)) {
            delayedFinish()
            return
        }

        toolbar = findViewById(R.id.toolbar)

        toolbar?.let {
            val toolbarColor = ContextCompat.getColor(this,
                    R.color.black_translucent_40
            )
            val drawable = ColorDrawable(toolbarColor)
            ViewCompat.setBackground(it, drawable)
        }

        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        fragmentContainer.visibility = View.VISIBLE
        showMainImageFragment()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MainImageFragment.ARG_MEDIA_ID, mediaId)
        outState.putString(MainImageFragment.ARG_MEDIA_CONTENT_URI, contentUri)
    }

    private fun delayedFinish() {
        Handler().postDelayed({ finish() }, 1500)
    }

    private fun showMainImageFragment() {
        contentUri?.let {
            val fragment = MainImageFragment.newInstance(it)

            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment, MainImageFragment.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit()
        }
    }

    companion object {
        fun startIntent(context: Context, intent: Intent) {
            val options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    R.anim.fade_in,
                    R.anim.fade_out
            )
            ActivityCompat.startActivity(context, intent, options.toBundle())
        }
    }
}
