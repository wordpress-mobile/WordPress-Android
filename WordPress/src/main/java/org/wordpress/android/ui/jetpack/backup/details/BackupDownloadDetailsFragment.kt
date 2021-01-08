package org.wordpress.android.ui.jetpack.backup.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.backup_download_details_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.backup.BackupDownloadViewModel
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsViewModel.UiState.Loading
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

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            when (uiState) {
                is Loading -> ToastUtils.showToast(requireContext(), "Implement loading")
                is Content -> showContent(uiState)
                is Error -> ToastUtils.showToast(requireContext(), "Implement Error")
            }
        })

        viewModel.start()
    }

    private fun showContent(content: Content) {
        ((recycler_view.adapter) as BackupDownloadDetailsAdapter).update(content.items)
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_DETAILS_FRAGMENT"
        fun newInstance(): BackupDownloadDetailsFragment {
            return BackupDownloadDetailsFragment()
        }
    }
}
