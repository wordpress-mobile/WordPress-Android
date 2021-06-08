package org.wordpress.android.ui.prefs.language

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.LocalePickerBottomSheetBinding
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class LocalePickerBottomSheet : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LocalePickerViewModel

    private val localeAdapter = LocalePickerAdapter()

    private var binding: LocalePickerBottomSheetBinding? = null

    private var bottomSheet: FrameLayout? = null

    private var callback: LocalePickerCallback? = null

    fun setLocalePickerCallback(callback: LocalePickerCallback?) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(LocalePickerViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LocalePickerBottomSheetBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            setupContentViews()
            setupObservers()
        }
    }

    private fun LocalePickerBottomSheetBinding.setupContentViews() {
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        recyclerView.adapter = localeAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    ActivityUtils.hideKeyboardForced(binding?.searchInputLayout)
                }
            }
        })

        searchInputLayout.editText?.doOnTextChanged() { _, _, _, _ ->
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
            viewModel.onSearchCancelled()
            ActivityUtils.hideKeyboardForced(binding?.searchInputLayout)
        }

//            btnTimezoneSuggestion.setOnClickListener { it as MaterialButton
//                timezoneViewModel.onTimezoneSelected(it.text.toString())
//            }

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) expandBottomSheet()
        }
    }

    private fun LocalePickerBottomSheetBinding.setupObservers() {
        viewModel.locales.observe(viewLifecycleOwner, {
            localeAdapter.submitList(it)
        })

        viewModel.selectedLocale.observe(viewLifecycleOwner, {
            callback?.onLocaleSelected(it)
            dismiss()
        })

        viewModel.showEmptyView.observe(viewLifecycleOwner, {
            emptyView.visibility = if (it) View.VISIBLE else View.GONE
        })


        viewModel.suggestedLocale.observe(viewLifecycleOwner, {
            btnLocaleSuggestion.text = it.localeCode
        })


        viewModel.dismissBottomSheet.observe(viewLifecycleOwner, {
            dismiss()
        })

        viewModel.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) expandBottomSheet()
    }

    override fun onDestroyView() {
        binding = null
        callback = null
        super.onDestroyView()
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
                viewModel.searchLocales(it)
            } else {
                viewModel.onSearchCancelled()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): LocalePickerBottomSheet = LocalePickerBottomSheet()
    }

    interface LocalePickerCallback {
        fun onLocaleSelected(languageCode: String)
    }
}
