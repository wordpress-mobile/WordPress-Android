package org.wordpress.android.ui.jetpack.backup.download.complete

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import javax.inject.Inject

class BackupDownloadCompleteFragment : Fragment(R.layout.backup_download_complete_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BackupDownloadCompleteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDagger()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadCompleteViewModel::class.java)
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_COMPLETE_FRAGMENT"
        fun newInstance(): BackupDownloadCompleteFragment {
            return BackupDownloadCompleteFragment()
        }
    }
}
