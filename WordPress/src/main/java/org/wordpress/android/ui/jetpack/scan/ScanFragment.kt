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
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.adapters.ScanAdapter
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsFragment
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
        initAdapter()
    }

    private fun initAdapter() {
        recycler_view.adapter = ScanAdapter(imageManager, uiHelpers)
        recycler_view.itemAnimator = null
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
                if (uiState is Content) {
                    refreshContentScreen(uiState)
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
                        is ShowThreatDetails -> ActivityLauncher.viewThreatDetailsForResult(
                            this@ScanFragment,
                            threatId
                        )
                    }
                }
            }
        )
    }

    private fun refreshContentScreen(content: Content) {
        ((recycler_view.adapter) as ScanAdapter).update(content.items)
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val snackbar = WPSnackbar.make(
            scan_state_layout,
            uiHelpers.getTextOfUiString(requireContext(), message),
            Snackbar.LENGTH_LONG
        )
        snackbar.show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.SHOW_THREAT_DETAILS &&
            data?.getBooleanExtra(ThreatDetailsFragment.REQUEST_SCAN_STATE, false) == true
        ) {
            viewModel.fetchScanState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val ARG_THREAT_ID = "arg_threat_id"
    }
}
