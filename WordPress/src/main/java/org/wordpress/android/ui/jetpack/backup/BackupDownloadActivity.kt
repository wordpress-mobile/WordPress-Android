package org.wordpress.android.ui.jetpack.backup

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.backup_download_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsFragment
import javax.inject.Inject

class BackupDownloadActivity : LocaleAwareActivity() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BackupDownloadViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.backup_download_activity)

        setSupportActionBar(toolbar_main)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            // todo: annmarie - this is here to get past lint - this will be set from childfrag
            it.title = getString(R.string.backup_download_details_page_title)
        }
        setupViewModel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadViewModel::class.java)
        viewModel.start()

        // todo: annmarie temporary start
        val fragment = BackupDownloadDetailsFragment.newInstance()
        showFragment(fragment, BackupDownloadDetailsFragment.TAG, slideIn = false, isRootFragment = true)
    }

    // todo: annmarie - decision pt: have activity/frag pairs or use the replace fragment approach
    private fun showFragment(
        fragment: Fragment,
        tag: String,
        slideIn: Boolean = true,
        isRootFragment: Boolean = false
    ) {
        val transaction = supportFragmentManager.beginTransaction()

        if (slideIn) {
            transaction.setCustomAnimations(
                    R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
            )
        }
        if (!isRootFragment) {
            transaction.addToBackStack(null)
        }

        transaction.replace(R.id.fragment_container, fragment, tag).commit()
    }
}
