package org.wordpress.android.ui.sitecreation.domains

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationDomainsScreenBinding
import org.wordpress.android.databinding.SiteCreationFormScreenBinding
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.domains.compose.SiteExample
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.SiteCreationHeaderUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSearchInputUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtilsWrapper
import javax.inject.Inject

@AndroidEntryPoint
class SiteCreationDomainsFragment : SiteCreationBaseFormFragment() {
    private var searchInputWithHeader: SearchInputWithHeader? = null
    private val viewModel: SiteCreationDomainsViewModel by activityViewModels()

    @Inject
    internal lateinit var uiHelpers: UiHelpers

    @Inject
    internal lateinit var displayUtils: DisplayUtilsWrapper

    private var binding: SiteCreationDomainsScreenBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is DomainsScreenListener) { "Parent activity must implement DomainsScreenListener." }
        check(context is OnHelpClickedListener) { "Parent activity must implement OnHelpClickedListener." }
    }

    override fun getContentLayout() = R.layout.site_creation_domains_screen

    override val screenTitle get() = requireArguments().getString(EXTRA_SCREEN_TITLE).orEmpty()

    override fun setBindingViewStubListener(parentBinding: SiteCreationFormScreenBinding) {
        parentBinding.siteCreationFormContentStub.setOnInflateListener { _, inflated ->
            binding = SiteCreationDomainsScreenBinding.bind(inflated)
        }
    }

    override fun setupContent() {
        binding?.let {
            searchInputWithHeader = SearchInputWithHeader(
                uiHelpers = uiHelpers,
                rootView = it.root as ViewGroup,
                onClear = viewModel::onClearTextBtnClicked,
            )
            it.createSiteButton.setOnClickListener { viewModel.onCreateSiteBtnClicked() }
            it.initRecyclerView()
            it.initViewModel()
            it.siteExampleComposeView.setContent {
                AppThemeM2 {
                    SiteExample()
                }
            }
        }
    }

    private fun SiteCreationDomainsScreenBinding.initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recyclerView.adapter = SiteCreationDomainsAdapter(uiHelpers)
    }

    private fun SiteCreationDomainsScreenBinding.initViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.let {
                searchInputWithHeader?.updateHeader(requireActivity(), uiState.headerUiState)
                searchInputWithHeader?.updateSearchInput(requireActivity(), uiState.searchInputUiState)
                updateContentUiState(uiState.contentState)
                createSiteButtonContainer.isVisible = uiState.createSiteButtonState != null
                createSiteButton.text = uiState.createSiteButtonState?.stringRes?.let(::getString)
                updateTitleVisibility(uiState.headerUiState == null)
            }
        }
        viewModel.clearBtnClicked.observe(viewLifecycleOwner) { searchInputWithHeader?.setInputText("") }
        viewModel.createSiteBtnClicked.observe(viewLifecycleOwner) { domain ->
            domain?.let { (requireActivity() as DomainsScreenListener).onDomainSelected(it) }
        }
        viewModel.onHelpClicked.observe(viewLifecycleOwner) {
            (requireActivity() as OnHelpClickedListener).onHelpClicked(HelpActivity.Origin.SITE_CREATION_DOMAINS)
        }
        viewModel.start()
    }

    private fun SiteCreationDomainsScreenBinding.updateContentUiState(contentState: DomainsUiContentState) {
        uiHelpers.updateVisibility(domainListEmptyView, contentState.emptyViewVisibility)
        if (contentState is DomainsUiContentState.Empty && contentState.message != null) {
            domainListEmptyViewMessage.text = uiHelpers.getTextOfUiString(requireContext(), contentState.message)
        }
        uiHelpers.updateVisibility(siteCreationDomainsScreenExample.root, contentState.exampleViewVisibility)
        uiHelpers.updateVisibility(siteExampleComposeView, contentState.updatedExampleViewVisibility)
        (recyclerView.adapter as SiteCreationDomainsAdapter).update(contentState.items)

        if (contentState.items.isNotEmpty()) {
            view?.announceForAccessibility(getString(R.string.suggestions_updated_content_description))
        }
    }

    private fun updateTitleVisibility(visible: Boolean) {
        val actionBar = (requireActivity() as? AppCompatActivity)?.supportActionBar
        actionBar?.setDisplayShowTitleEnabled(displayUtils.isLandscapeBySize() || visible)
    }

    override fun onHelp() = viewModel.onHelpClicked()

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // we need to set the `onTextChanged` after the viewState has been restored otherwise the viewModel.updateQuery
        // is called when the system sets the restored value to the EditText which results in an unnecessary request
        searchInputWithHeader?.onTextChanged = { viewModel.onQueryChanged(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchInputWithHeader = null
        binding = null
    }

    private class SearchInputWithHeader(private val uiHelpers: UiHelpers, rootView: View, onClear: () -> Unit) {
        private val headerLayout = rootView.findViewById<ViewGroup>(R.id.site_creation_header_item)
        private val headerTitle = rootView.findViewById<TextView>(R.id.title)
        private val headerSubtitle = rootView.findViewById<TextView>(R.id.subtitle)
        private val searchInput = rootView.findViewById<EditText>(R.id.input)
        private val progressBar = rootView.findViewById<View>(R.id.progress_bar)
        private val clearAllLayout = rootView.findViewById<View>(R.id.clear_all_layout)
        private val divider = rootView.findViewById<View>(R.id.divider)
        private val showKeyboardHandler = Handler(Looper.getMainLooper())

        var onTextChanged: ((String) -> Unit)? = null

        init {
            clearAllLayout.setOnClickListener {
                onClear()
            }

            searchInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = Unit
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged?.invoke(s?.toString() ?: "")
                }
            })
        }

        fun setInputText(text: String) {
            // If the text hasn't changed avoid triggering the text watcher
            if (searchInput.text.toString() != text) {
                searchInput.setText(text)
            }
        }

        fun updateHeader(context: Context, uiState: SiteCreationHeaderUiState?) {
            val headerShouldBeVisible = uiState != null
            if (!headerShouldBeVisible && headerLayout.isVisible) {
                headerLayout.animate().translationY(-headerLayout.height.toFloat())
            } else if (headerShouldBeVisible && headerLayout.isGone) {
                headerLayout.animate().translationY(0f)
            }
            uiState?.let {
                uiHelpers.updateVisibility(headerLayout, true)
                headerTitle.text = uiHelpers.getTextOfUiString(context, uiState.title)
                headerSubtitle.text = uiHelpers.getTextOfUiString(context, uiState.subtitle)
            } ?: uiHelpers.updateVisibility(headerLayout, false)
        }

        fun updateSearchInput(context: Context, uiState: SiteCreationSearchInputUiState) {
            searchInput.hint = uiHelpers.getTextOfUiString(context, uiState.hint)
            uiHelpers.updateVisibility(progressBar, uiState.showProgress)
            uiHelpers.updateVisibility(clearAllLayout, uiState.showClearButton)
            uiHelpers.updateVisibility(divider, uiState.showDivider)
            showKeyboard(uiState.showKeyboard)
        }

        private fun showKeyboard(shouldShow: Boolean) {
            if (shouldShow) {
                searchInput.requestFocus()
                /**
                 * This workaround handles the case where the SiteCreationDomainsFragment appears after the
                 * DesignPreviewFragment dismisses and the keyboard fails to appear
                 */
                showKeyboardHandler.postDelayed({ ActivityUtils.showKeyboard(searchInput) }, SHOW_KEYBOARD_DELAY)
            }
        }
    }

    companion object {
        const val TAG = "site_creation_domains_fragment_tag"
        const val SHOW_KEYBOARD_DELAY = 200L

        fun newInstance(screenTitle: String): SiteCreationDomainsFragment {
            val fragment = SiteCreationDomainsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
