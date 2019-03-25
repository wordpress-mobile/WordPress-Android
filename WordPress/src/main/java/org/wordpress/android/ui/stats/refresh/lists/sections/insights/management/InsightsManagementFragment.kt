package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.insights_management_fragment.*
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel
import javax.inject.Inject

class InsightsManagementFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: InsightsManagementViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.insights_management_fragment, container, false)
    }

    private fun initializeViews() {
        removedInsights.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        initializeViews()
        initializeViewModels(nonNullActivity)
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(InsightsManagementViewModel::class.java)
        setupObservers(activity)

        viewModel.start()
    }

    private fun setupObservers(activity: FragmentActivity) {
        viewModel.showSnackbarMessage.observe(this, Observer { holder ->
            val parent = activity.findViewById<View>(R.id.coordinatorLayout)
            if (holder != null && parent != null) {
                if (holder.buttonTitleRes == null) {
                    Snackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG).show()
                } else {
                    val snackbar = Snackbar.make(parent, getString(holder.messageRes), Snackbar.LENGTH_LONG)
                    snackbar.setAction(getString(holder.buttonTitleRes)) { holder.buttonAction() }
                    snackbar.show()
                }
            }
        })

        viewModel.removedInsights.observe(this, Observer {
            it?.let { items ->
                updateRemovedInsights(items)
            }
        })
    }

    private fun updateRemovedInsights(insights: List<InsightModel>) {
        val adapter: InsightsManagementAdapter
        if (removedInsights.adapter == null) {
            adapter = InsightsManagementAdapter()
            removedInsights.adapter = adapter
        } else {
            adapter = removedInsights.adapter as InsightsManagementAdapter
        }
        adapter.submitList(insights)
    }
}
