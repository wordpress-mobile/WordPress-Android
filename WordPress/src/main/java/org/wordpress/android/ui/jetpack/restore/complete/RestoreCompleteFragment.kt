package org.wordpress.android.ui.jetpack.restore.complete

import android.os.Bundle
import android.view.View
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
import org.wordpress.android.ui.jetpack.restore.complete.RestoreCompleteViewModel.UiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val ARG_DATA = "arg_restore_complete_data"

class RestoreCompleteFragment : Fragment(R.layout.jetpack_backup_restore_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var parentViewModel: RestoreViewModel
    private lateinit var viewModel: RestoreCompleteViewModel

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
        recycler_view.adapter = JetpackBackupRestoreAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(RestoreViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(RestoreCompleteViewModel::class.java)

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
        fun newInstance(
            bundle: Bundle? = null,
            restoreState: RestoreState
        ): RestoreCompleteFragment {
            val newBundle = Bundle().apply {
                putParcelable(ARG_DATA, restoreState)
            }
            bundle?.let { newBundle.putAll(bundle) }
            return RestoreCompleteFragment().apply { arguments = newBundle }
        }
    }
}
