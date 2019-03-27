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
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightModel
import javax.inject.Inject
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.wordpress.android.R

class InsightsManagementFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: InsightsManagementViewModel
    private lateinit var addedInsightsTouchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.insights_management_fragment, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.menu_insights_management, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save_insights) {
            viewModel.onSaveInsights()
        }
        return true
    }

    private fun initializeViews() {
        removedInsights.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        addedInsights.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
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

        viewModel.addedInsights.observe(this, Observer {
            it?.let { items ->
                updateAddedInsights(items)
            }
        })
    }

    private fun updateRemovedInsights(insights: List<InsightModel>) {
        if (removedInsights.adapter == null) {
            removedInsights.adapter = InsightsManagementAdapter()
        }
        val adapter = removedInsights.adapter as InsightsManagementAdapter
        adapter.update(insights)
    }

    private fun updateAddedInsights(insights: List<InsightModel>) {
        var adapter = addedInsights.adapter as? InsightsManagementAdapter
        if (adapter == null) {
            adapter = InsightsManagementAdapter { viewHolder -> addedInsightsTouchHelper.startDrag(viewHolder) }
            addedInsights.adapter = adapter

            val callback = ItemTouchHelperCallback(adapter)
            addedInsightsTouchHelper = ItemTouchHelper(callback)
            addedInsightsTouchHelper.attachToRecyclerView(addedInsights)
        }
        adapter.update(insights)
    }
}
