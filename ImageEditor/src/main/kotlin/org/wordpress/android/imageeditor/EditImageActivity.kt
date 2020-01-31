package org.wordpress.android.imageeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EditImageActivity : AppCompatActivity() {
    private lateinit var hostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val navController: NavController
        get() = hostFragment.navController

    private var editor: ImageEditor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_image)

        editor = intent.getSerializableExtra("editor") as ImageEditor?

        GlobalScope.launch {
            editor?.load("https://test287home.wpcomstaging.com/wp-content/uploads/2019/10/mountaindawn.jpg")
        }

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
}
