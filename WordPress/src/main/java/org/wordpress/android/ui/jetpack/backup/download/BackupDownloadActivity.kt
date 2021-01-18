package org.wordpress.android.ui.jetpack.backup.download

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.jetpack_backup_restore_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.DownloadFile
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.ShareLink
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.COMPLETE
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.DETAILS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.PROGRESS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsFragment
import org.wordpress.android.ui.jetpack.backup.download.complete.BackupDownloadCompleteFragment
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

const val KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID = "key_backup_download_download_id"

class BackupDownloadActivity : LocaleAwareActivity() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: BackupDownloadViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.jetpack_backup_restore_activity)

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

        viewModel.snackbarEvents.observe(this, {
            it?.applyIfNotHandled {
                showSnackbar()
            }
        })

        viewModel.errorEvents.observe(this, {
            it?.applyIfNotHandled {
                viewModel.transitionToError(this)
            }
        })

        viewModel.wizardFinishedObservable.observe(this, {
            it.applyIfNotHandled {
                val intent = Intent()
                val (backupDownloadCreated, downloadId) = when (this) {
                    is BackupDownloadCanceled -> Pair(false, null)
                    is BackupDownloadInProgress -> Pair(true, downloadId)
                    is BackupDownloadCompleted -> Pair(true, null)
                }
                intent.putExtra(KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID, downloadId)
                setResult(if (backupDownloadCreated) RESULT_OK else RESULT_CANCELED, intent)
                finish()
            }
        })

        viewModel.navigationEvents.observe(this, {
            it.applyIfNotHandled {
                when (this) {
                    is ShareLink -> {
                        ActivityLauncher.shareBackupDownloadFileLink(this@BackupDownloadActivity, url)
                    }
                    is DownloadFile -> {
                        ActivityLauncher.downloadBackupDownloadFile(this@BackupDownloadActivity, url)
                    }
                }
            }
        })

        viewModel.start(savedInstanceState)
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val snackbar = WPSnackbar.make(
                coordinator_layout,
                uiHelpers.getTextOfUiString(this@BackupDownloadActivity, message),
                Snackbar.LENGTH_LONG
        )
        if (buttonTitle != null) {
            snackbar.setAction(
                    uiHelpers.getTextOfUiString(this@BackupDownloadActivity, buttonTitle)
            ) {
                buttonAction.invoke()
            }
        }
        snackbar.show()
    }

    private fun showStep(target: WizardNavigationTarget<BackupDownloadStep, BackupDownloadState>) {
        val fragment = when (target.wizardStep) {
            DETAILS -> BackupDownloadDetailsFragment.newInstance(intent?.extras)
            PROGRESS -> BackupDownloadProgressFragment.newInstance(intent?.extras, target.wizardState)
            COMPLETE -> BackupDownloadCompleteFragment.newInstance(intent?.extras, target.wizardState)
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
