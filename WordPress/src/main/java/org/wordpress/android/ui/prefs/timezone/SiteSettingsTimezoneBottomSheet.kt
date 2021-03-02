package org.wordpress.android.ui.prefs.timezone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteSettingsTimezoneBottomSheetListBinding
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class SiteSettingsTimezoneBottomSheet : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var timezoneViewModel: SiteSettingsTimezoneViewModel

    private val timezoneAdapter = TimezoneAdapter { timezone ->
        setSelectedTimezone(timezone)
    }

    private var _binding: SiteSettingsTimezoneBottomSheetListBinding? = null
    private val binding get() = _binding

    private var callback: TimezoneSelectionCallback? = null

    fun setTimezoneSettingCallback(callback: TimezoneSelectionCallback) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        timezoneViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(SiteSettingsTimezoneViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SiteSettingsTimezoneBottomSheetListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        setupLiveData()
        timezoneViewModel.requestTimezones(requireActivity())
    }

    private fun setupUI() {
        binding.apply {
            this?.list?.adapter = timezoneAdapter
            this?.list?.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))

            this?.searchView?.setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        timezoneViewModel.searchTimezones(it)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        timezoneViewModel.searchTimezones(it)
                    }
                    return true
                }
            })

            this?.searchView?.setOnCloseListener {
                timezoneViewModel.onSearchCancelled()
                hideSearchKeyboard()
                true
            }
        }
    }

    private fun setupLiveData() {
        timezoneViewModel.showEmptyView.observe(viewLifecycleOwner, {
            showEmptyView(it)
        })

        timezoneViewModel.showProgressView.observe(viewLifecycleOwner, {
            showProgressView(it)
        })

        timezoneViewModel.timezones.observe(viewLifecycleOwner, {
            timezoneAdapter.submitList(it)
            timezoneAdapter.notifyDataSetChanged()
            binding?.searchView?.isEnabled = true
        })

        timezoneViewModel.timezoneSearch.observe(viewLifecycleOwner, {
            timezoneAdapter.submitList(it)
            timezoneAdapter.notifyDataSetChanged()
            hideSearchKeyboard()
        })

        timezoneViewModel.dismissWithError.observe(viewLifecycleOwner, {
            dismissWithError()
        })
    }

    override fun onDestroyView() {
        _binding = null
        callback = null
        super.onDestroyView()
    }

    private fun setSelectedTimezone(timezone: Timezone?) {
        timezone?.let {
            callback?.onSelectTimezone(it.value)
            dismiss()
        }
    }

    private fun showEmptyView(show: Boolean) {
        if (isAdded) {
            binding?.emptyView?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun showProgressView(show: Boolean) {
        if (isAdded) {
            binding?.progressView?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun hideSearchKeyboard() {
        if (isAdded) {
            ActivityUtils.hideKeyboardForced(binding?.searchView)
        }
    }

    private fun dismissWithError() {
        ToastUtils.showToast(activity, R.string.site_settings_timezones_error)
        dismiss()
    }

    companion object {
        const val KEY_TIMEZONE = "timezone"

        @JvmStatic
        fun newInstance(timezone: String): SiteSettingsTimezoneBottomSheet =
                SiteSettingsTimezoneBottomSheet().apply {
                    arguments = Bundle().apply {
                        putString(KEY_TIMEZONE, timezone)
                    }
                }
    }

    interface TimezoneSelectionCallback {
        fun onSelectTimezone(timezone: String)
    }
}

