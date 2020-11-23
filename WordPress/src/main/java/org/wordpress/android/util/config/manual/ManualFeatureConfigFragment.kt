package org.wordpress.android.util.config.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.manual_feature_config_fragment.*
import org.wordpress.android.R
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject
import kotlin.system.exitProcess

class ManualFeatureConfigFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ManualFeatureConfigViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.manual_feature_config_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recycler_view.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(ManualFeatureConfigViewModel::class.java)
        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            it?.let { uiState ->
                val adapter: FeatureAdapter
                if (recycler_view.adapter == null) {
                    adapter = FeatureAdapter()
                    recycler_view.adapter = adapter
                } else {
                    adapter = recycler_view.adapter as FeatureAdapter
                }

                val layoutManager = recycler_view?.layoutManager
                val recyclerViewState = layoutManager?.onSaveInstanceState()
                adapter.update(uiState.uiItems)
                layoutManager?.onRestoreInstanceState(recyclerViewState)
            }
        })
        viewModel.restartAction.observe(viewLifecycleOwner, Observer {
            it?.applyIfNotHandled {
                exitProcess(0)
            }
        })
        viewModel.start()
    }
}
