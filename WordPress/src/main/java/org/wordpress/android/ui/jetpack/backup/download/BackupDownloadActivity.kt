package org.wordpress.android.ui.jetpack.backup.download

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.backup_download_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.COMPLETE
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.DETAILS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.PROGRESS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsFragment
import org.wordpress.android.ui.jetpack.backup.download.complete.BackupDownloadCompleteFragment
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressFragment
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.wizard.WizardNavigationTarget
import javax.inject.Inject

class BackupDownloadActivity : LocaleAwareActivity() {
    // todo: annmarie add listeners if needed
    // todo: annmarie get the values from the bundle for site & activityId
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: BackupDownloadViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.backup_download_activity)

        setSupportActionBar(toolbar_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViewModel(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadViewModel::class.java)

        viewModel.navigationTargetObservable.observe(this, { target ->
            target?.let {
                showStep(target)
            }
        })

        viewModel.toolbarStateObservable.observe(this, { state ->
            supportActionBar?.title = getString(state.title)
            supportActionBar?.setHomeAsUpIndicator(state.icon)
            // Change the activity title for accessibility purposes
            this.title = getString(state.title)
        })

        // Canceled, Running, Complete -> (Running = kick off status)
        viewModel.wizardFinishedObservable.observe(this, {
            it.applyIfNotHandled {
                val intent = Intent()
                val (backupDownloadCreated, _) = when (this) {
                    // teh request was canceled
                    is BackupDownloadCanceled -> Pair(false, null)
                    is BackupDownloadInProgress -> Pair(true, activityId)
                    is BackupDownloadCompleted -> Pair(true, activityId)
                }
                // todo: annmarie what information do I need to send back - just to kick off status
                // intent.putExtra(SOME_KEY_THAT_DESCRIBES_THE_ID, activityId )
                setResult(if (backupDownloadCreated) RESULT_OK else RESULT_CANCELED, intent)
                finish()
            }
        })

        viewModel.exitFlowObservable.observe(this, {
            setResult(Activity.RESULT_CANCELED)
            finish()
        })
        viewModel.onBackPressedObservable.observe(this, {
            super.onBackPressed()
        })
        viewModel.start(savedInstanceState)
    }

    private fun showStep(target: WizardNavigationTarget<BackupDownloadStep, BackupDownloadState>) {
        val fragment = when (target.wizardStep) {
            DETAILS -> BackupDownloadDetailsFragment.newInstance()
            PROGRESS -> BackupDownloadProgressFragment.newInstance()
            COMPLETE -> BackupDownloadCompleteFragment.newInstance()
        }
        slideInFragment(fragment, target.wizardStep.toString())
    }

    private fun slideInFragment(fragment: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            transaction.addToBackStack(null).setCustomAnimations(
                    R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
            )
        }
        transaction.replace(R.id.fragment_container, fragment, tag)
        transaction.commit()
    }
}
