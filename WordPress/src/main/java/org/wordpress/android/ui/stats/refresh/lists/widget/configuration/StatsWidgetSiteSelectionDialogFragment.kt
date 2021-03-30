package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.R
import org.wordpress.android.databinding.StatsWidgetSiteSelectorBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class StatsWidgetSiteSelectionDialogFragment : AppCompatDialogFragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsSiteSelectionViewModel
    private fun buildView(): View {
        val rootView = requireActivity().layoutInflater.inflate(R.layout.stats_widget_site_selector, null)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = StatsWidgetSiteAdapter(imageManager)
        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
        val view = buildView()
        alertDialogBuilder.setView(view)
        alertDialogBuilder.setTitle(R.string.stats_widget_select_your_site)
        alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> }
        alertDialogBuilder.setCancelable(true)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(StatsSiteSelectionViewModel::class.java)
        viewModel.sites.observe(this, {
            with(StatsWidgetSiteSelectorBinding.bind(view)) {
                (recyclerView.adapter as? StatsWidgetSiteAdapter)?.update(it ?: listOf())
            }
        })
        viewModel.hideSiteDialog.observeEvent(this, {
            if (dialog?.isShowing == true) {
                requireDialog().dismiss()
            }
        })
        viewModel.loadSites()
        return alertDialogBuilder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }
}
