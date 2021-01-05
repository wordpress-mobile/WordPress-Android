package org.wordpress.android.ui.jetpack.backup.download.progress

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.backup_download_progress_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.progress.BackupDownloadProgressViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.progress.adapters.BackupDownloadProgressAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val ARG_DATA = "arg_backup_download_progress_data"

class BackupDownloadProgressFragment : Fragment(R.layout.backup_download_progress_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var parentViewModel: BackupDownloadViewModel
    private lateinit var viewModel: BackupDownloadProgressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDagger()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun initAdapter() {
        recycler_view.adapter = BackupDownloadProgressAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(BackupDownloadViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadProgressViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            when (uiState) {
                is Content -> showContent(uiState)
                is Error -> ToastUtils.showToast(requireContext(), "Implement Error")
            }
        })

        val (site, state) = when {
            arguments != null -> {
                val site = requireNotNull(arguments).getSerializable(WordPress.SITE) as SiteModel
                val state = requireNotNull(arguments)
                        .getParcelable<BackupDownloadState>(ARG_DATA) as BackupDownloadState
                site to state
            }
            else -> throw Throwable("Couldn't initialize ${javaClass.simpleName} view model")
        }

        viewModel.start(site, state, parentViewModel)
    }

    private fun showContent(content: Content) {
        ((recycler_view.adapter) as BackupDownloadProgressAdapter).update(content.items)
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_PROGRESS_FRAGMENT"
        fun newInstance(
            bundle: Bundle? = null,
            backupDownloadState: BackupDownloadState
        ): BackupDownloadProgressFragment {
            val newBundle = Bundle().apply {
                putParcelable(ARG_DATA, backupDownloadState)
            }
            bundle?.let { newBundle.putAll(bundle) }
            return BackupDownloadProgressFragment().apply { arguments = newBundle }
        }
    }
}
