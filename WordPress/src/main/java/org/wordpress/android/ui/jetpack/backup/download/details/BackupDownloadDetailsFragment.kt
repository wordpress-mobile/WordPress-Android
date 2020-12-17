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
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Error
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.download.details.BackupDownloadDetailsViewModel.UiState.Loading
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class BackupDownloadDetailsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
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
        recycler_view.adapter = BackupDownloadDetailsAdapter(uiHelpers)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(BackupDownloadViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadDetailsViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            when (uiState) {
                is Loading -> ToastUtils.showToast(requireContext(), "Implement loading")
                is Content -> showContent(uiState)
                is Error -> ToastUtils.showToast(requireContext(), "Implement Error")
            }
        })

        parentViewModel.setToolbarState(DetailsToolbarState())
        viewModel.start()
    }

    private fun showContent(content: Content) {
        ((recycler_view.adapter) as BackupDownloadDetailsAdapter).update(content.items)
    }

    // todo: annmarie -(REMOVE) this dummy method references layout files that lint says aren't
    // used, but they will be in the next PR because there were too many changes for 1 PR
    private fun dummyAccess() {
        val buttonLayoutId = R.layout.backup_download_list_button_item
        val checkboxLayoutId = R.layout.backup_download_list_checkbox_item
        val descriptionLayoutId = R.layout.backup_download_list_description_item
        val headerLayoutId = R.layout.backup_download_list_header_item
        val subheaderLayoutId = R.layout.backup_download_list_subheader_item
        val imageLayoutId = R.layout.backup_download_list_image_item
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_DETAILS_FRAGMENT"
        fun newInstance(): BackupDownloadDetailsFragment {
            return BackupDownloadDetailsFragment()
        }
    }
}
