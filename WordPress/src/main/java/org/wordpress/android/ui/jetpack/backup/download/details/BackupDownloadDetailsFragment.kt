package org.wordpress.android.ui.jetpack.backup.download.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.backup_download_details_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.backup.download.details.adapters.BackupDownloadDetailsAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class BackupDownloadDetailsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var parentViewModel: BackupDownloadViewModel
    private lateinit var viewModel: BackupDownloadDetailsViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.backup_download_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component().inject(this)

        initRecyclerView()
        initViewModel()
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun initAdapter() {
        recycler_view.adapter = BackupDownloadDetailsAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(BackupDownloadViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadDetailsViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, { showView(it) })

        val (site, activityId) = when {
            arguments != null -> {
                val site = requireNotNull(arguments).getSerializable(WordPress.SITE) as SiteModel
                val activityId = requireNotNull(arguments).getString(
                        KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY
                ) as String
                site to activityId
            }
            else -> throw Throwable("Couldn't initialize BackupDownloadDetails view model")
        }

        viewModel.start(site, activityId, parentViewModel)
    }

    private fun showView(content: UiState) {
        ((recycler_view.adapter) as BackupDownloadDetailsAdapter).update(content.items)
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_DETAILS_FRAGMENT"
        fun newInstance(bundle: Bundle?): BackupDownloadDetailsFragment {
            return BackupDownloadDetailsFragment().apply { arguments = bundle }
        }
    }
}
