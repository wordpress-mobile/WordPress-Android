package org.wordpress.android.ui.jetpack.restore.warning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.jetpack_backup_restore_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.common.adapters.JetpackBackupRestoreAdapter
import org.wordpress.android.ui.jetpack.restore.RestoreState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import org.wordpress.android.ui.jetpack.restore.warning.RestoreWarningViewModel.UiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val ARG_DATA = "arg_restore_warning_data"

class RestoreWarningFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var parentViewModel: RestoreViewModel
    private lateinit var viewModel: RestoreWarningViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.jetpack_backup_restore_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
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
        recycler_view.adapter = JetpackBackupRestoreAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(RestoreViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(RestoreWarningViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, { showView(it) })

        val (site, state) = when {
            arguments != null -> {
                val site = requireNotNull(arguments).getSerializable(WordPress.SITE) as SiteModel
                val state = requireNotNull(arguments)
                        .getParcelable<RestoreState>(ARG_DATA) as RestoreState
                site to state
            }
            else -> throw Throwable("Couldn't initialize ${javaClass.simpleName} view model")
        }

        viewModel.start(site, state, parentViewModel)
    }

    private fun showView(uiState: UiState) {
        ((recycler_view.adapter) as JetpackBackupRestoreAdapter).update(uiState.items)
    }

    companion object {
        const val TAG = "RESTORE_WARNING_FRAGMENT"
        fun newInstance(
            bundle: Bundle? = null,
            restoreState: RestoreState
        ): RestoreWarningFragment {
            val newBundle = Bundle().apply {
                putParcelable(ARG_DATA, restoreState)
            }
            bundle?.let { newBundle.putAll(bundle) }
            return RestoreWarningFragment().apply { arguments = newBundle }
        }
    }
}
