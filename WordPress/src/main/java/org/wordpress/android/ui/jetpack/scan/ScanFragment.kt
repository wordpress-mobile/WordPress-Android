package org.wordpress.android.ui.jetpack.scan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ScanFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.accounts.HelpActivity.Origin.SCAN_SCREEN_HELP
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowContactSupport
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.VisitVaultPressDashboard
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.FullScreenLoadingUiState
import org.wordpress.android.ui.jetpack.scan.adapters.HorizontalMarginItemDecoration
import org.wordpress.android.ui.jetpack.scan.adapters.ScanAdapter
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.EmptyViewRecyclerView
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

@AndroidEntryPoint
class ScanFragment : Fragment(R.layout.scan_fragment) {
    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var listView: EmptyViewRecyclerView
    private var fixThreatsConfirmationDialog: AlertDialog? = null
    private val viewModel: ScanViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(ScanFragmentBinding.bind(view)) {
            initRecyclerView()
            listView = recyclerView
            initViewModel(getSite(savedInstanceState))
        }
    }

    override fun onResume() {
        super.onResume()
        if (activity is ScrollableViewInitializedListener) {
            (activity as ScrollableViewInitializedListener).onScrollableViewInitialized(listView.id)
        }
    }

    private fun ScanFragmentBinding.initRecyclerView() {
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(
            HorizontalMarginItemDecoration(resources.getDimensionPixelSize(R.dimen.margin_extra_large))
        )
        initAdapter()
        initActionableEmptyView()
    }

    private fun ScanFragmentBinding.initActionableEmptyView() {
        recyclerView.setEmptyView(actionableEmptyView)
        uiHelpers.updateVisibility(actionableEmptyView, false)
    }

    private fun ScanFragmentBinding.initAdapter() {
        recyclerView.adapter = ScanAdapter(imageManager, uiHelpers)
    }

    private fun ScanFragmentBinding.initViewModel(site: SiteModel) {
        setupObservers()
        viewModel.start(site)
    }

    private fun ScanFragmentBinding.setupObservers() {
        viewModel.uiState.observe(
            viewLifecycleOwner,
            { uiState ->
                uiHelpers.updateVisibility(progressBar, uiState.loadingVisible)
                uiHelpers.updateVisibility(recyclerView, uiState.contentVisible)
                uiHelpers.updateVisibility(actionableEmptyView, uiState.errorVisible)

                when (uiState) {
                    is ContentUiState -> updateContentLayout(uiState)

                    is FullScreenLoadingUiState -> { // Do Nothing
                    }

                    is ErrorUiState.NoConnection,
                    is ErrorUiState.GenericRequestFailed,
                    is ErrorUiState.ScanRequestFailed,
                    is ErrorUiState.MultisiteNotSupported,
                    is ErrorUiState.VaultPressActiveOnSite -> updateErrorLayout(uiState as ErrorUiState)
                }
            }
        )

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, { it.showSnackbar() })

        viewModel.navigationEvents.observeEvent(
            viewLifecycleOwner
        ) { events ->
            when (events) {
                is OpenFixThreatsConfirmationDialog -> showFixThreatsConfirmationDialog(events)

                is ShowThreatDetails -> ActivityLauncher.viewThreatDetails(
                    this@ScanFragment,
                    events.siteModel,
                    events.threatId
                )

                is ShowContactSupport ->
                    ActivityLauncher.viewHelp(requireContext(), SCAN_SCREEN_HELP, events.site, null)

                is ShowJetpackSettings -> ActivityLauncher.openUrlExternal(context, events.url)

                is VisitVaultPressDashboard -> ActivityLauncher.openUrlExternal(context, events.url)
            }
        }
    }

    private fun ScanFragmentBinding.updateContentLayout(state: ContentUiState) {
        ((recyclerView.adapter) as ScanAdapter).update(state.items)
    }

    private fun ScanFragmentBinding.updateErrorLayout(state: ErrorUiState) {
        uiHelpers.setTextOrHide(actionableEmptyView.title, state.title)
        uiHelpers.setTextOrHide(actionableEmptyView.subtitle, state.subtitle)
        actionableEmptyView.image.setImageResource(state.image)
        state.imageColorResId?.let {
            ColorUtils.setImageResourceWithTint(actionableEmptyView.image, state.image, it)
        } ?: actionableEmptyView.image.setImageResource(state.image)
        state.buttonText?.let { uiHelpers.setTextOrHide(actionableEmptyView.button, state.buttonText) }
        state.action?.let { action -> actionableEmptyView.button.setOnClickListener { action.invoke() } }
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
