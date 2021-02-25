package org.wordpress.android.ui.jetpack.scan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.scan_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.HelpActivity.Origin.SCAN_SCREEN_HELP
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowContactSupport
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.FullScreenLoadingUiState
import org.wordpress.android.ui.jetpack.scan.adapters.HorizontalMarginItemDecoration
import org.wordpress.android.ui.jetpack.scan.adapters.ScanAdapter
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class ScanFragment : Fragment(R.layout.scan_fragment) {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ScanViewModel
    private var fixThreatsConfirmationDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initRecyclerView()
        initViewModel(getSite(savedInstanceState))
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initRecyclerView() {
        recycler_view.itemAnimator = null
        recycler_view.addItemDecoration(
            HorizontalMarginItemDecoration(resources.getDimensionPixelSize(R.dimen.margin_extra_large))
        )
        recycler_view.setEmptyView(actionable_empty_view)
        initAdapter()
    }

    private fun initAdapter() {
        recycler_view.adapter = ScanAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ScanViewModel::class.java)
        setupObservers()
        viewModel.start(site)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(
            viewLifecycleOwner,
            { uiState ->
                uiHelpers.updateVisibility(progress_bar, uiState.loadingVisible)
                uiHelpers.updateVisibility(recycler_view, uiState.contentVisible)
                uiHelpers.updateVisibility(actionable_empty_view, uiState.errorVisible)

                when (uiState) {
                    is ContentUiState -> updateContentLayout(uiState)

                    is FullScreenLoadingUiState -> { // Do Nothing
                    }

                    is ErrorUiState.NoConnection,
                    is ErrorUiState.GenericRequestFailed,
                    is ErrorUiState.ScanRequestFailed -> updateErrorLayout(uiState as ErrorUiState)
                }
            }
        )

        viewModel.snackbarEvents.observe(viewLifecycleOwner, { it?.applyIfNotHandled { showSnackbar() } })

        viewModel.navigationEvents.observe(
            viewLifecycleOwner,
            {
                it.applyIfNotHandled {
                    when (this) {
                        is OpenFixThreatsConfirmationDialog -> showFixThreatsConfirmationDialog(this)

                        is ShowThreatDetails -> ActivityLauncher.viewThreatDetails(
                            this@ScanFragment,
                            siteModel,
                            threatId
                        )

                        is ShowContactSupport ->
                            ActivityLauncher.viewHelpAndSupport(requireContext(), SCAN_SCREEN_HELP, this.site, null)
                    }
                }
            }
        )
    }

    private fun updateContentLayout(state: ContentUiState) {
        ((recycler_view.adapter) as ScanAdapter).update(state.items)
    }

    private fun updateErrorLayout(state: ErrorUiState) {
        uiHelpers.setTextOrHide(actionable_empty_view.title, state.title)
        uiHelpers.setTextOrHide(actionable_empty_view.subtitle, state.subtitle)
        uiHelpers.setTextOrHide(actionable_empty_view.button, state.buttonText)
        actionable_empty_view.image.setImageResource(state.image)
        actionable_empty_view.button.setOnClickListener { state.action.invoke() }
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        view?.let {
            val snackbar = WPSnackbar.make(
                it,
                uiHelpers.getTextOfUiString(requireContext(), message),
                Snackbar.LENGTH_LONG
            )
            snackbar.show()
        }
    }

    private fun showFixThreatsConfirmationDialog(holder: OpenFixThreatsConfirmationDialog) {
        fixThreatsConfirmationDialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(uiHelpers.getTextOfUiString(requireContext(), holder.title))
            .setMessage(uiHelpers.getTextOfUiString(requireContext(), holder.message))
            .setPositiveButton(holder.positiveButtonLabel) { _, _ -> holder.okButtonAction.invoke() }
            .setNegativeButton(holder.negativeButtonLabel) { _, _ -> fixThreatsConfirmationDialog?.dismiss() }
            .setCancelable(true)
            .create()
        fixThreatsConfirmationDialog?.show()
    }

    override fun onPause() {
        super.onPause()
        fixThreatsConfirmationDialog?.dismiss()
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    fun onNewIntent(intent: Intent?) {
        intent?.let {
            val threatId = intent.getLongExtra(ScanActivity.REQUEST_FIX_STATE, 0L)
            val messageRes = intent.getIntExtra(ScanActivity.REQUEST_SCAN_STATE, 0)
            if (threatId > 0L) {
                viewModel.onFixStateRequested(threatId)
            } else if (messageRes > 0) {
                viewModel.onScanStateRequestedWithMessage(messageRes)
            }
        }
    }

    companion object {
        const val ARG_THREAT_ID = "arg_threat_id"
    }
}
