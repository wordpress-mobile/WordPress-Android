package org.wordpress.android.imageeditor

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.navigation.NavArgument
import androidx.navigation.fragment.NavHostFragment

import org.wordpress.android.imageeditor.fragments.MainImageFragment

class EditImageActivity : AppCompatActivity() {
    private var contentUri: String? = null
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.edit_image_activity)

        if (savedInstanceState != null) {
            contentUri = savedInstanceState.getString(MainImageFragment.ARG_MEDIA_CONTENT_URI)
        } else {
            contentUri = intent.getStringExtra(MainImageFragment.ARG_MEDIA_CONTENT_URI)
        }

        if (TextUtils.isEmpty(contentUri)) {
            delayedFinish()
            return
        }

        toolbar = findViewById(R.id.toolbar)

        toolbar?.let {
            val toolbarColor = ContextCompat.getColor(
                this,
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

        val host: NavHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return

        val navController = host.navController

        contentUri?.let {
            val graph = navController.navInflater.inflate(R.navigation.mobile_navigation)

            val navArgument = NavArgument.Builder().setDefaultValue(contentUri).build()
            graph.addArgument(MainImageFragment.ARG_MEDIA_CONTENT_URI, navArgument)

            navController.graph = graph
        }
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
        outState.putString(MainImageFragment.ARG_MEDIA_CONTENT_URI, contentUri)
    }

    private fun delayedFinish() {
        Handler().postDelayed({ finish() }, 1500)
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
