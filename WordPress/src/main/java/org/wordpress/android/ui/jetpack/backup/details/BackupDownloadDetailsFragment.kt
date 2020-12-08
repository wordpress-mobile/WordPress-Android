package org.wordpress.android.ui.jetpack.backup.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.backup.BackupDownloadViewModel
import javax.inject.Inject

class BackupDownloadDetailsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
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

        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(BackupDownloadViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BackupDownloadDetailsViewModel::class.java)

        setupViews()
        setupObservers()
        // todo: annmarie - if something needs to be passed to VM, do it on start
        viewModel.start()
    }

    private fun setupViews() {
        // TODO: annmarie implement setupViews
    }

    private fun setupObservers() {
        // TODO: annmarie implement setupObservers
    }

    companion object {
        const val TAG = "BACKUP_DOWNLOAD_DETAILS_FRAGMENT"
        fun newInstance(): BackupDownloadDetailsFragment {
            return BackupDownloadDetailsFragment()
        }
    }
}
