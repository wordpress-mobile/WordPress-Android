package org.wordpress.android.ui.jetpack.restore

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.restore_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreStep.COMPLETE
import org.wordpress.android.ui.jetpack.restore.RestoreStep.DETAILS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.PROGRESS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.WARNING
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCompleted
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreInProgress
import org.wordpress.android.ui.jetpack.restore.complete.RestoreCompleteFragment
import org.wordpress.android.ui.jetpack.restore.details.RestoreDetailsFragment
import org.wordpress.android.ui.jetpack.restore.progress.RestoreProgressFragment
import org.wordpress.android.ui.jetpack.restore.warning.RestoreWarningFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

const val KEY_RESTORE_RESTORE_ID = "key_restore_restore_id"

class RestoreActivity : LocaleAwareActivity() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: RestoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.restore_activity)

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
                .get(RestoreViewModel::class.java)

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
                val (restoreCreated, restoreId) = when (this) {
                    is RestoreCanceled -> Pair(false, null)
                    is RestoreInProgress -> Pair(true, restoreId)
                    is RestoreCompleted -> Pair(true, null)
                }
                intent.putExtra(KEY_RESTORE_RESTORE_ID, restoreId)
                setResult(if (restoreCreated) RESULT_OK else RESULT_CANCELED, intent)
                finish()
            }
        })

        viewModel.navigationEvents.observe(this, {
            it.applyIfNotHandled {
                when (this) {
                    is VisitSite -> {
                        ActivityLauncher.openUrlExternal(this@RestoreActivity, url)
                    }
                }
            }
        })

        viewModel.onBackPressedObservable.observe(this, {
            super.onBackPressed()
        })

        viewModel.start(savedInstanceState)
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val snackbar = WPSnackbar.make(
                coordinator_layout,
                uiHelpers.getTextOfUiString(this@RestoreActivity, message),
                Snackbar.LENGTH_LONG
        )
        if (buttonTitle != null) {
            snackbar.setAction(
                    uiHelpers.getTextOfUiString(this@RestoreActivity, buttonTitle)
            ) {
                buttonAction.invoke()
            }
        }
        snackbar.show()
    }

    private fun showStep(target: WizardNavigationTarget<RestoreStep, RestoreState>) {
        val fragment = when (target.wizardStep) {
            DETAILS -> RestoreDetailsFragment.newInstance(intent?.extras, target.wizardState)
            WARNING -> RestoreWarningFragment.newInstance(intent?.extras, target.wizardState)
            PROGRESS -> RestoreProgressFragment.newInstance(intent?.extras, target.wizardState)
            COMPLETE -> RestoreCompleteFragment.newInstance(intent?.extras, target.wizardState)
        }

        slideInFragment(fragment, target.wizardStep.toString())
    }

    private fun slideInFragment(fragment: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            transaction.addToBackStack(null).setCustomAnimations(
                    R.anim.activity_slide_in_from_right,
                    R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left,
                    R.anim.activity_slide_out_to_right
            )
        }
        transaction.replace(R.id.fragment_container, fragment, tag)
        transaction.commit()
    }
}
