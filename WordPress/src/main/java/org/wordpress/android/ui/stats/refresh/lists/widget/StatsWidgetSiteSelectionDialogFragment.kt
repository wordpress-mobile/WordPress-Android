package org.wordpress.android.ui.stats.refresh.lists.widget

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.stats_widget_site_selector.*
import org.wordpress.android.R
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class StatsWidgetSiteSelectionDialogFragment : AppCompatDialogFragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ViewsWidgetViewModel
    private fun buildView(): View? {
        val rootView = activity!!.layoutInflater.inflate(R.layout.stats_widget_site_selector, null)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = StatsWidgetSiteAdapter(imageManager)
        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder.setView(buildView())
        alertDialogBuilder.setTitle(R.string.stats_widget_select_your_site)
        alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> }
        alertDialogBuilder.setCancelable(true)

        viewModel = ViewModelProviders.of(activity!!, viewModelFactory).get(ViewsWidgetViewModel::class.java)
        viewModel.sites.observe(this, Observer {
            (dialog.recycler_view.adapter as? StatsWidgetSiteAdapter)?.update(it ?: listOf())
        })
        viewModel.hideSiteDialog.observe(this, Observer {
            it?.applyIfNotHandled {
                if (dialog?.isShowing == true) {
                    dialog.dismiss()
                }
            }
        })
        viewModel.loadSites()
        return alertDialogBuilder.create()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }
}
