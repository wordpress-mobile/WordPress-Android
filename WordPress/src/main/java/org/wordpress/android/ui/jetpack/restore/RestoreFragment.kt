package org.wordpress.android.ui.jetpack.restore

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
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.JetpackBackupRestoreFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.jetpack.common.adapters.JetpackBackupRestoreAdapter
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCompleted
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreInProgress
import org.wordpress.android.ui.jetpack.scan.adapters.HorizontalMarginItemDecoration
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

const val KEY_RESTORE_REWIND_ID = "key_restore_rewind_id"
const val KEY_RESTORE_RESTORE_ID = "key_restore_restore_id"

class RestoreFragment : Fragment(R.layout.jetpack_backup_restore_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var uiHelpers: UiHelpers
    @Inject
    lateinit var imageManager: ImageManager
    private lateinit var viewModel: RestoreViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(JetpackBackupRestoreFragmentBinding.bind(view)) {
            initDagger()
            initBackPressHandler()
            initAdapter()
            initViewModel(savedInstanceState)
        }
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

    private fun JetpackBackupRestoreFragmentBinding.initAdapter() {
        recyclerView.adapter = JetpackBackupRestoreAdapter(imageManager, uiHelpers)
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(
            HorizontalMarginItemDecoration(resources.getDimensionPixelSize(dimen.margin_extra_large))
        )
    }

    private fun JetpackBackupRestoreFragmentBinding.initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this@RestoreFragment, viewModelFactory).get(RestoreViewModel::class.java)

        val (site, activityId) = when {
            requireActivity().intent?.extras != null -> {
                val site = requireNotNull(requireActivity().intent.extras).getSerializable(WordPress.SITE) as SiteModel
                val activityId = requireNotNull(requireActivity().intent.extras).getString(
                    KEY_RESTORE_ACTIVITY_ID_KEY
                ) as String
                site to activityId
            }
            else -> {
                AppLog.e(T.JETPACK_REWIND, "Error initializing ${this.javaClass.simpleName}")
                throw Throwable("Couldn't initialize ${this.javaClass.simpleName} view model")
            }
        }

        initObservers()

        viewModel.start(site, activityId, savedInstanceState)
    }

    private fun JetpackBackupRestoreFragmentBinding.initObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, {
            updateToolbar(it.toolbarState)
            showView(it)
        })

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, {
            it.showSnackbar()
        })

        viewModel.navigationEvents.observeEvent(viewLifecycleOwner, { events ->
            when (events) {
                is VisitSite -> ActivityLauncher.openUrlExternal(requireContext(), events.url)
                is ShowJetpackSettings -> ActivityLauncher.openUrlExternal(requireContext(), events.url)
            }
        })

        viewModel.wizardFinishedObservable.observeEvent(viewLifecycleOwner, { state ->
            val intent = Intent()
            val (restoreCreated, ids) = when (state) {
                is RestoreCanceled -> Pair(false, null)
                is RestoreInProgress -> Pair(true, Pair(state.rewindId, state.restoreId))
                is RestoreCompleted -> Pair(true, null)
            }
            intent.putExtra(KEY_RESTORE_REWIND_ID, ids?.first)
            intent.putExtra(KEY_RESTORE_RESTORE_ID, ids?.second)
            requireActivity().let { activity ->
                activity.setResult(if (restoreCreated) RESULT_OK else RESULT_CANCELED, intent)
                activity.finish()
            }
        })
    }

    private fun JetpackBackupRestoreFragmentBinding.showView(state: RestoreUiState) {
        ((recyclerView.adapter) as JetpackBackupRestoreAdapter).update(state.items)
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
