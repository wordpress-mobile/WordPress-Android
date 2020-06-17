package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.insights_management_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel.InsightListItem
import javax.inject.Inject

class InsightsManagementFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: InsightsManagementViewModel

    private var menu: Menu? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.insights_management_fragment, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_insights_management, menu)
        this.menu = menu
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val siteId = activity?.intent?.getIntExtra(WordPress.LOCAL_SITE_ID, 0)
        initializeViews()
        initializeViewModels(requireActivity(), siteId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save_insights) {
            viewModel.onSaveInsights()
        }
        return true
    }

    private fun initializeViews() {
        insightCards.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
    }

    private fun initializeViewModels(activity: FragmentActivity, siteId: Int?) {
        viewModel = ViewModelProviders.of(activity, viewModelFactory).get(InsightsManagementViewModel::class.java)

        viewModel.start(siteId)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.addedInsights.observe(viewLifecycleOwner, Observer {
            it?.let { items ->
                updateAddedInsights(items)

                if (items.isEmpty()) {
                    insightCards.visibility = View.GONE
                } else {
                    insightCards.visibility = View.VISIBLE
                }
            }
        })

        viewModel.closeInsightsManagement.observe(viewLifecycleOwner, Observer {
            requireActivity().finish()
        })

        viewModel.isMenuVisible.observe(viewLifecycleOwner, Observer { isMenuVisible ->
            isMenuVisible?.let {
                menu?.findItem(R.id.save_insights)?.isVisible = isMenuVisible
            }
        })
    }

    private fun updateAddedInsights(insights: List<InsightListItem>) {
        var adapter = insightCards.adapter as? InsightsManagementAdapter
        if (adapter == null) {
            adapter = InsightsManagementAdapter()
            insightCards.adapter = adapter
        }
        adapter.update(insights)
    }
}
