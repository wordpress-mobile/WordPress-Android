package org.wordpress.android.ui.jetpack.backup.download

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.jetpack_backup_restore_fragment.recycler_view
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.DownloadFile
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.ShareLink
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.jetpack.common.adapters.JetpackBackupRestoreAdapter
import org.wordpress.android.ui.jetpack.scan.adapters.HorizontalMarginItemDecoration
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

const val KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID = "key_backup_download_download_id"

class BackupDownloadFragment : Fragment(R.layout.jetpack_backup_restore_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: BackupDownloadViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDagger()
        initBackPressHandler()
        initAdapter()
        initViewModel(savedInstanceState)
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(
                        true
                ) {
                    override fun handleOnBackPressed() {
                        onBackPressed()
                    }
                })
    }

    private fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun initAdapter() {
        recycler_view.adapter = JetpackBackupRestoreAdapter(imageManager, uiHelpers)
        recycler_view.itemAnimator = null
        recycler_view.addItemDecoration(
                HorizontalMarginItemDecoration(resources.getDimensionPixelSize(R.dimen.margin_extra_large))
        )
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(BackupDownloadViewModel::class.java)

        val (site, activityId) = when {
            requireActivity().intent?.extras != null -> {
                val site = requireNotNull(requireActivity().intent.extras).getSerializable(WordPress.SITE) as SiteModel
                val activityId = requireNotNull(requireActivity().intent.extras).getString(
                        KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY
                ) as String
                site to activityId
            }
            else -> throw Throwable("Couldn't initialize ${this.javaClass.simpleName} view model")
        }

        initObservers()

        viewModel.start(site, activityId, savedInstanceState)
    }

    private fun initObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, {
            updateToolbar(it.toolbarState)
            showView(it)
        })

        viewModel.snackbarEvents.observe(viewLifecycleOwner, {
            it?.applyIfNotHandled {
                showSnackbar()
            }
        })

        viewModel.navigationEvents.observe(viewLifecycleOwner, {
            it.applyIfNotHandled {
                when (this) {
                    is ShareLink -> {
                        ActivityLauncher.shareBackupDownloadFileLink(requireContext(), url)
                    }
                    is DownloadFile -> {
                        ActivityLauncher.downloadBackupDownloadFile(requireContext(), url)
                    }
                }
            }
        })

        viewModel.wizardFinishedObservable.observe(viewLifecycleOwner, {
            it.applyIfNotHandled {
                val intent = Intent()
                val (backupDownloadCreated, downloadId) = when (this) {
                    is BackupDownloadCanceled -> Pair(false, null)
                    is BackupDownloadInProgress -> Pair(true, downloadId)
                    is BackupDownloadCompleted -> Pair(true, null)
                }
                intent.putExtra(KEY_BACKUP_DOWNLOAD_DOWNLOAD_ID, downloadId)
                requireActivity().setResult(
                        if (backupDownloadCreated) RESULT_OK else RESULT_CANCELED,
                        intent
                )
                requireActivity().finish()
            }
        })
    }

    private fun showView(state: BackupDownloadUiState) {
        ((recycler_view.adapter) as JetpackBackupRestoreAdapter).update(state.items)
    }

    private fun updateToolbar(toolbarState: ToolbarState) {
        val activity = requireActivity() as? AppCompatActivity
        activity?.supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.title = getString(toolbarState.title)
            it.setHomeAsUpIndicator(toolbarState.icon)
        }
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        activity?.findViewById<View>(R.id.coordinator_layout)?.let { coordinator ->
            val snackbar = WPSnackbar.make(
                    coordinator,
                    uiHelpers.getTextOfUiString(requireContext(), this.message),
                    Snackbar.LENGTH_LONG
            )
            if (this.buttonTitle != null) {
                snackbar.setAction(
                        uiHelpers.getTextOfUiString(
                                requireContext(),
                                this.buttonTitle
                        )
                ) {
                    this.buttonAction.invoke()
                }
            }
            snackbar.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }
}
