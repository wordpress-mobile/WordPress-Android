package org.wordpress.android.ui.jetpack.restore.progress

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
import org.wordpress.android.ui.jetpack.restore.RestoreState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import org.wordpress.android.ui.jetpack.restore.progress.RestoreProgressViewModel.UiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val ARG_DATA = "arg_restore_restore_progress_data"

class RestoreProgressFragment : Fragment(R.layout.jetpack_backup_restore_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var imageManager: ImageManager
    private lateinit var parentViewModel: RestoreViewModel
    private lateinit var viewModel: RestoreProgressViewModel

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
        recycler_view.adapter = RestoreProgressAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(RestoreViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(RestoreProgressViewModel::class.java)

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
        ((recycler_view.adapter) as RestoreProgressAdapter).update(uiState.items)
    }

    companion object {
        const val TAG = "RESTORE_PROGRESS_FRAGMENT"
        fun newInstance(
            bundle: Bundle? = null,
            restoreState: RestoreState
        ): RestoreProgressFragment {
            val newBundle = Bundle().apply { putParcelable(ARG_DATA, restoreState) }
            bundle?.let { newBundle.putAll(bundle) }
            return RestoreProgressFragment().apply { arguments = newBundle }
        }
    }
}
