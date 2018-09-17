package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import org.wordpress.android.WordPress
import javax.inject.Inject

class StatsListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity?.application as WordPress).component()?.inject(this)

        val viewModelProvider = ViewModelProviders.of(this, viewModelFactory)
        viewModel = viewModelProvider.get(StatsListViewModel::class.java)

        val adapter = StatsListAdapter(viewModelProvider)

        viewModel.data.observe(this, Observer {
            if (it != null) {
                adapter.update(it)
            }
        })
    }
}
