package org.wordpress.android.ui.jetpack.backup.progress

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.backup.details.BackupDownloadDetailsFragment
import javax.inject.Inject

class BackupDownloadProgressFragment : Fragment(R.layout.backup_download_progress_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BackupDownloadProgressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
                this,
                viewModelFactory
        ).get(BackupDownloadProgressViewModel::class.java)
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_PROGRESS_FRAGMENT"
        fun newInstance(): BackupDownloadProgressFragment {
            return BackupDownloadProgressFragment()
        }
    }
}
