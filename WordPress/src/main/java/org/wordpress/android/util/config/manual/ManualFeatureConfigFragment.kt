package org.wordpress.android.util.config.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.ManualFeatureConfigFragmentBinding
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject
import kotlin.system.exitProcess

class ManualFeatureConfigFragment : DaggerFragment(R.layout.manual_feature_config_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ManualFeatureConfigViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(ManualFeatureConfigFragmentBinding.bind(view)) {
            recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))

            viewModel = ViewModelProvider(this@ManualFeatureConfigFragment, viewModelFactory)
                    .get(ManualFeatureConfigViewModel::class.java)
            viewModel.uiState.observe(viewLifecycleOwner, {
                it?.let { uiState ->
                    val adapter: FeatureAdapter
                    if (recyclerView.adapter == null) {
                        adapter = FeatureAdapter()
                        recyclerView.adapter = adapter
                    } else {
                        adapter = recyclerView.adapter as FeatureAdapter
                    }

                    val layoutManager = recyclerView.layoutManager
                    val recyclerViewState = layoutManager?.onSaveInstanceState()
                    adapter.update(uiState.uiItems)
                    layoutManager?.onRestoreInstanceState(recyclerViewState)
                }
            })
            viewModel.restartAction.observeEvent(viewLifecycleOwner, {
                exitProcess(0)
            })
            viewModel.start()
        }
    }
}
