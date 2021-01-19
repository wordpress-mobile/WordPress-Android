package org.wordpress.android.ui.jetpack.scan.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.threat_details_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.scan.ScanFragment.Companion.ARG_THREAT_ID
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.OpenIgnoreThreatActionDialog
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.ShowUpdatedScanState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.adapters.ThreatDetailsAdapter
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class ThreatDetailsFragment : Fragment(R.layout.threat_details_fragment) {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ThreatDetailsViewModel
    private var threatActionDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initAdapter()
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initAdapter() {
        recycler_view.adapter = ThreatDetailsAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ThreatDetailsViewModel::class.java)
        setupObservers()
        val threatId = requireActivity().intent.getLongExtra(ARG_THREAT_ID, 0)
        viewModel.start(threatId)
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
                        is OpenIgnoreThreatActionDialog -> showThreatActionDialog(this)

                        is ShowUpdatedScanState -> {
                            val intent = Intent().putExtra(REQUEST_SCAN_STATE, true)
                            activity?.setResult(Activity.RESULT_OK, intent)
                            activity?.finish()
                        }
                    }
                }
            }
        )
    }

    private fun refreshContentScreen(content: Content) {
        ((recycler_view.adapter) as ThreatDetailsAdapter).update(content.items)
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val snackbar = WPSnackbar.make(
            threat_details_layout,
            uiHelpers.getTextOfUiString(requireContext(), message),
            Snackbar.LENGTH_LONG
        )
        snackbar.show()
    }

    private fun showThreatActionDialog(holder: OpenIgnoreThreatActionDialog) {
        threatActionDialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(uiHelpers.getTextOfUiString(requireContext(), holder.title))
            .setMessage(uiHelpers.getTextOfUiString(requireContext(), holder.message))
            .setPositiveButton(holder.positiveButtonLabel) { _, _ -> holder.okButtonAction.invoke() }
            .setNegativeButton(holder.negativeButtonLabel) { _, _ -> threatActionDialog?.dismiss() }
            .setCancelable(true)
            .create()
        threatActionDialog?.show()
    }

    override fun onPause() {
        super.onPause()
        threatActionDialog?.dismiss()
    }

    companion object {
        const val REQUEST_SCAN_STATE = "request_scan_state"
    }
}
