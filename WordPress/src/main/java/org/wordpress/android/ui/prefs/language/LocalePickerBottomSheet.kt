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
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(LocalePickerViewModel::class.java)
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

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val sheet = dialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) as? FrameLayout
                sheet?.let {
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.skipCollapsed = true
                }
            }
        }
    }

    private fun LocalePickerBottomSheetBinding.setupContentViews() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        recyclerView.adapter = localeAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    viewModel.onListScrolled()
                }
            }
        })

        searchInputLayout.editText?.doOnTextChanged { _, _, _, _ ->
            onSearchStatusUpdated()
        }

        searchInputLayout.editText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.onSearchFieldFocused()
            }
        }

        searchInputLayout.editText?.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                onSearchStatusUpdated()
                true
            } else {
                false
            }
        }

        searchInputLayout.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onSearchStatusUpdated()
                true
            } else {
                false
            }
        }

        searchInputLayout.setEndIconOnClickListener {
            viewModel.onClearSearchFieldButtonClicked()
        }

        btnLocaleSuggestion.setOnClickListener {
            viewModel.onCurrentLocaleSelected()
        }

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog
            bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout
        }
    }

    private fun LocalePickerBottomSheetBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.let {
                localeAdapter.submitList(uiState.listData)
                btnLocaleSuggestion.text = uiState.currentLocale?.label
                emptyView.visibility = if (uiState.isEmptyViewVisible) View.VISIBLE else View.GONE
            }
        }

        viewModel.hideKeyboard.observe(viewLifecycleOwner) {
            ActivityUtils.hideKeyboardForced(binding?.searchInputLayout)
        }

        viewModel.expandBottomSheet.observe(viewLifecycleOwner) {
            expandBottomSheet()
        }

        viewModel.clearSearchField.observe(viewLifecycleOwner) {
            searchInputLayout.editText?.text?.clear()
            searchInputLayout.editText?.clearFocus()
        }

        viewModel.selectedLocale.observe(viewLifecycleOwner) {
            callback?.onLocaleSelected(it)
        }

        viewModel.dismissBottomSheet.observe(viewLifecycleOwner) {
            dismiss()
        }

        viewModel.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        callback = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        } else {
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = false
            }
        }
    }

    private fun expandBottomSheet() {
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun onSearchStatusUpdated() {
        binding?.searchInputLayout?.editText?.text?.trim().let {
            viewModel.onSearchQueryChanged(it)
        }
    }

    companion object {
        const val TAG = "LOCALE_PICKER_TAG"
        @JvmStatic
        fun newInstance(): LocalePickerBottomSheet = LocalePickerBottomSheet()
    }

    interface LocalePickerCallback {
        fun onLocaleSelected(languageCode: String)
    }
}
