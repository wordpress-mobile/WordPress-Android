package org.wordpress.android.imageeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.navigateUp
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorCancelled

class EditImageActivity : AppCompatActivity() {
    private lateinit var viewModel: EditImageViewModel
    private lateinit var hostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val navController: NavController
        get() = hostFragment.navController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(EditImageViewModel::class.java)
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
        return if (navController.currentDestination?.id == R.id.crop_dest) {
            // Using popUpToInclusive for popping the start destination of the graph off the back stack
            // in a multi-module project doesn't seem to be working. Explicitly invoking back action as a workaround.
            // Related issue: https://issuetracker.google.com/issues/147312109
            onBackPressed()
            true
        } else {
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (hostFragment.childFragmentManager.backStackEntryCount == 0) {
            ImageEditor.instance.onEditorAction(EditorCancelled)
        }
    }
}
