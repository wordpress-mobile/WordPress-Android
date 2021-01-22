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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.jetpack_backup_restore_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.jetpack.common.adapters.JetpackBackupRestoreAdapter
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCompleted
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreInProgress
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

const val KEY_RESTORE_RESTORE_ID = "key_restore_restore_id"

class RestoreFragment : Fragment(R.layout.jetpack_backup_restore_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var viewModel: RestoreViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDagger()
        initBackPressHandler()
        initRecyclerView()
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

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun initAdapter() {
        recycler_view.adapter = JetpackBackupRestoreAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(RestoreViewModel::class.java)

        val (site, activityId) = when {
            requireActivity().intent?.extras != null -> {
                val site = requireNotNull(requireActivity().intent.extras).getSerializable(WordPress.SITE) as SiteModel
                val activityId = requireNotNull(requireActivity().intent.extras).getString(
                        KEY_RESTORE_ACTIVITY_ID_KEY
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
                    is VisitSite -> ActivityLauncher.openUrlExternal(requireContext(), url)
                }
            }
        })

        viewModel.wizardFinishedObservable.observe(viewLifecycleOwner, {
            it.applyIfNotHandled {
                val intent = Intent()
                val (restoreCreated, restoreId) = when (this) {
                    is RestoreCanceled -> Pair(false, null)
                    is RestoreInProgress -> Pair(true, restoreId)
                    is RestoreCompleted -> Pair(true, null)
                }
                intent.putExtra(KEY_RESTORE_RESTORE_ID, restoreId)
                requireActivity().setResult(
                        if (restoreCreated) RESULT_OK else RESULT_CANCELED,
                        intent
                )
                requireActivity().finish()
            }
        })
    }

    private fun showView(state: RestoreUiState) {
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

    companion object {
        const val TAG = "RESTORE_FRAGMENT"
        fun newInstance(bundle: Bundle?): RestoreFragment {
            return RestoreFragment().apply { arguments = bundle }
        }
    }
}
