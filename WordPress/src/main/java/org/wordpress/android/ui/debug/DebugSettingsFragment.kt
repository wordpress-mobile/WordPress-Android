package org.wordpress.android.ui.debug

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugSettingsFragmentBinding
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

@AndroidEntryPoint
class DebugSettingsFragment : Fragment(R.layout.debug_settings_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DebugSettingsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(DebugSettingsFragmentBinding.bind(view)) {
            viewModel = ViewModelProvider(this@DebugSettingsFragment, viewModelFactory)
                .get(DebugSettingsViewModel::class.java)

            val adapter = DebugSettingsAdapter()
            setUpRecyclerView(adapter)

            viewModel.uiState.observe(viewLifecycleOwner) {
                it?.let { uiState ->
                    adapter.submitList(uiState.uiItems)
                }
            }
            viewModel.start(getDebugSettingsType())
        }
    }

    private fun getDebugSettingsType() = arguments?.getSerializableCompat<DebugSettingsType>(
        DEBUG_SETTINGS_TYPE_KEY
    ) ?: throw IllegalArgumentException(
        "DebugSettingsType not provided"
    )

    private fun setUpRecyclerView(adapter: DebugSettingsAdapter) {
        with(DebugSettingsFragmentBinding.bind(requireView())) {
            recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(requireActivity(), 1)))
            recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            recyclerView.adapter = adapter
        }
    }

    companion object {
        private const val DEBUG_SETTINGS_TYPE_KEY = "debug_settings_type_key"
        fun newInstance(debugSettingsType: DebugSettingsType) = DebugSettingsFragment().apply {
            arguments = Bundle().apply {
                putSerializable(DEBUG_SETTINGS_TYPE_KEY, debugSettingsType)
            }
        }
    }
}
