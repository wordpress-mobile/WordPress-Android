package org.wordpress.android.ui.debug

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugSettingsFragmentBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.debug.DebugSettingsViewModel.NavigationAction.DebugCookies
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class DebugSettingsFragment : DaggerFragment(R.layout.debug_settings_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DebugSettingsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(DebugSettingsFragmentBinding.bind(view)) {
            with(requireActivity() as AppCompatActivity) {
                setSupportActionBar(toolbar)
                supportActionBar?.let {
                    it.setHomeButtonEnabled(true)
                    it.setDisplayHomeAsUpEnabled(true)
                }
            }
            recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))

            viewModel = ViewModelProvider(this@DebugSettingsFragment, viewModelFactory)
                .get(DebugSettingsViewModel::class.java)
            viewModel.uiState.observe(viewLifecycleOwner, {
                it?.let { uiState ->
                    val adapter: DebugSettingsAdapter
                    if (recyclerView.adapter == null) {
                        adapter = DebugSettingsAdapter()
                        recyclerView.adapter = adapter
                    } else {
                        adapter = recyclerView.adapter as DebugSettingsAdapter
                    }

                    val layoutManager = recyclerView.layoutManager
                    val recyclerViewState = layoutManager?.onSaveInstanceState()
                    adapter.submitList(uiState.uiItems)
                    layoutManager?.onRestoreInstanceState(recyclerViewState)
                }
            })
            viewModel.onNavigation.observeEvent(viewLifecycleOwner) {
                when (it) {
                    DebugCookies -> ActivityLauncher.viewDebugCookies(requireContext())
                }
            }
            viewModel.start()
        }
    }
}
