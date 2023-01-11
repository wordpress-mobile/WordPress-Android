package org.wordpress.android.ui.prefs.timezone

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteSettingsTimezoneBottomSheetListBinding
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class SiteSettingsTimezoneBottomSheet : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var timezoneViewModel: SiteSettingsTimezoneViewModel

    private val timezoneAdapter = TimezoneAdapter { timezone ->
        timezoneViewModel.onTimezoneSelected(timezone.value)
    }

    private var binding: SiteSettingsTimezoneBottomSheetListBinding? = null

    private var bottomSheet: FrameLayout? = null

    private var callback: TimezoneSelectionCallback? = null

    fun setTimezoneSettingCallback(callback: TimezoneSelectionCallback?) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        timezoneViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(SiteSettingsTimezoneViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SiteSettingsTimezoneBottomSheetListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        setupLiveData()
        timezoneViewModel.getTimezones(requireContext())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) expandBottomSheet()
    }

    override fun onDestroyView() {
        callback = null
        super.onDestroyView()
        binding = null
    }

    private fun setupUI() {
        binding?.apply {
            dialog?.setOnShowListener { dialogInterface ->
                val sheetDialog = dialogInterface as? BottomSheetDialog

                bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) as? FrameLayout

                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) expandBottomSheet()
            }

            list.adapter = timezoneAdapter
            list.run {
                addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            ActivityUtils.hideKeyboardForced(binding?.searchInputLayout)
                        }
                    }
                })
            }

            searchInputLayout.editText?.doOnTextChanged { _, _, _, _ ->
                searchTimezones()
            }

            searchInputLayout.editText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) expandBottomSheet()
            }

            searchInputLayout.editText?.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    searchTimezones()
                    true
                } else {
                    false
                }
            }

            searchInputLayout.editText?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    searchTimezones()
                    true
                } else {
                    false
                }
            }

            searchInputLayout.setEndIconOnClickListener {
                searchInputLayout.editText?.text?.clear()
                searchInputLayout.editText?.clearFocus()
                timezoneViewModel.onSearchCancelled()
                ActivityUtils.hideKeyboardForced(binding?.searchInputLayout)
            }

            btnTimezoneSuggestion.setOnClickListener {
                it as MaterialButton
                timezoneViewModel.onTimezoneSelected(it.text.toString())
            }
        }
    }

    private fun setupLiveData() {
        timezoneViewModel.showEmptyView.observe(viewLifecycleOwner, {
            binding?.emptyView?.visibility = if (it) View.VISIBLE else View.GONE
        })

        timezoneViewModel.showProgressView.observe(viewLifecycleOwner, {
            binding?.progressView?.visibility = if (it) View.VISIBLE else View.GONE
        })

        timezoneViewModel.timezones.observe(viewLifecycleOwner, {
            timezoneAdapter.submitList(it)
        })

        timezoneViewModel.suggestedTimezone.observe(viewLifecycleOwner, {
            binding?.btnTimezoneSuggestion?.text = it
        })

        timezoneViewModel.dismissWithError.observe(viewLifecycleOwner, {
            ToastUtils.showToast(activity, R.string.site_settings_timezones_error)
            dismiss()
        })

        timezoneViewModel.selectedTimezone.observe(viewLifecycleOwner, {
            callback?.onSelectTimezone(it)
        })

        timezoneViewModel.dismissBottomSheet.observe(viewLifecycleOwner, {
            dismiss()
        })
    }

    private fun expandBottomSheet() {
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun searchTimezones() {
        binding?.searchInputLayout?.editText?.text?.trim().let {
            if (it?.isNotEmpty() == true) {
                timezoneViewModel.searchTimezones(it)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): SiteSettingsTimezoneBottomSheet = SiteSettingsTimezoneBottomSheet()
    }

    interface TimezoneSelectionCallback {
        fun onSelectTimezone(timezone: String)
    }
}
