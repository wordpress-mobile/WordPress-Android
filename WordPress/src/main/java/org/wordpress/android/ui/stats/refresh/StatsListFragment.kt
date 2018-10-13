package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_log_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.refresh.StatsListViewModel.StatsListType
import javax.inject.Inject

class StatsListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var mViewModel: StatsListViewModel

    private lateinit var viewModel: StatsListViewModel
    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(listType: StatsListType): StatsListFragment {
            val fragment = StatsListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, listType)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_log_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log_list_view.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        (activity?.application as WordPress).component()?.inject(this)

        val viewModelProvider = ViewModelProviders.of(this, viewModelFactory)
        mViewModel = viewModelProvider.get(StatsListViewModel::class.java)

        val adapter = InsightsAdapter()

        log_list_view.adapter = adapter

        mViewModel.data.observe(this, Observer {
            if (it != null) {
                adapter.update(it.data)
            }
        })
        mViewModel.start()
    }
}
