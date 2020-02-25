package org.wordpress.android.imageeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.yalantis.ucrop.UCropFragment.UCropResult
import com.yalantis.ucrop.UCropFragmentCallback

class EditImageActivity : AppCompatActivity(), UCropFragmentCallback {
    private lateinit var hostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val navController: NavController
        get() = hostFragment.navController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_image)

        hostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
                ?: throw(NullPointerException("Host fragment is null inside ${this::class.java.simpleName} onCreate."))

        setupActionBar()
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Passing in an empty set of top-level destination to display back button on the start destination
        appBarConfiguration = AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
            // Handle app bar's back button on start destination
            onBackPressed()
            true
        }.build()

        setupActionBarWithNavController(this, navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        // Allows NavigationUI to support proper up navigation
        return (navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp())
    }

    override fun onCropFinish(result: UCropResult?) { }

    override fun loadingProgress(showLoader: Boolean) { }
}
