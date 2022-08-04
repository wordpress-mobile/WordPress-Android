package org.wordpress.android.ui.sitecreation.domains

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationDomainsScreenBinding
import org.wordpress.android.databinding.SiteCreationFormScreenBinding
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.SearchInputWithHeader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtilsWrapper
import javax.inject.Inject

@AndroidEntryPoint
class SiteCreationDomainsFragment : SiteCreationBaseFormFragment() {
    private var searchInputWithHeader: SearchInputWithHeader? = null
    private val viewModel: SiteCreationDomainsViewModel by activityViewModels()

    @Inject internal lateinit var uiHelpers: UiHelpers
    @Inject internal lateinit var displayUtils: DisplayUtilsWrapper

    private var binding: SiteCreationDomainsScreenBinding? = null

    @Suppress("UseCheckOrError")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is DomainsScreenListener) {
            throw IllegalStateException("Parent activity must implement DomainsScreenListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
    }

    override fun getContentLayout(): Int {
        return R.layout.site_creation_domains_screen
    }

    @Suppress("UseCheckOrError") override val screenTitle: String
        get() = arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")

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
                    onClear = { viewModel.onClearTextBtnClicked() }
            )
            it.createSiteButton.setOnClickListener { viewModel.createSiteBtnClicked() }
            it.initRecyclerView()
            it.initViewModel()
        }
    }

    private fun SiteCreationDomainsScreenBinding.initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun SiteCreationDomainsScreenBinding.initAdapter() {
        val adapter = SiteCreationDomainsAdapter(uiHelpers)
        recyclerView.adapter = adapter
    }

    private fun SiteCreationDomainsScreenBinding.initViewModel() {
        viewModel.uiState.observe(this@SiteCreationDomainsFragment, { uiState ->
            uiState?.let {
                searchInputWithHeader?.updateHeader(requireActivity(), uiState.headerUiState)
                searchInputWithHeader?.updateSearchInput(requireActivity(), uiState.searchInputUiState)
                updateContentUiState(uiState.contentState)
                uiHelpers.updateVisibility(createSiteButtonContainer, uiState.createSiteButtonContainerVisibility)
                uiHelpers.updateVisibility(createSiteButtonShadow, uiState.createSiteButtonContainerVisibility)
                updateTitleVisibility(uiState.headerUiState == null)
            }
        })
        viewModel.clearBtnClicked.observe(this@SiteCreationDomainsFragment, {
            searchInputWithHeader?.setInputText("")
        })
        viewModel.createSiteBtnClicked.observe(this@SiteCreationDomainsFragment, { domain ->
            domain?.let { (requireActivity() as DomainsScreenListener).onDomainSelected(domain) }
        })
        viewModel.onHelpClicked.observe(this@SiteCreationDomainsFragment, {
            (requireActivity() as OnHelpClickedListener).onHelpClicked(HelpActivity.Origin.SITE_CREATION_DOMAINS)
        })
        viewModel.start()
    }

    private fun SiteCreationDomainsScreenBinding.updateContentUiState(contentState: DomainsUiContentState) {
        uiHelpers.updateVisibility(domainListEmptyView, contentState.emptyViewVisibility)
        if (contentState is DomainsUiContentState.Empty && contentState.message != null) {
            domainListEmptyViewMessage.text = uiHelpers.getTextOfUiString(requireContext(), contentState.message)
        }
        uiHelpers.updateVisibility(siteCreationDomainsScreenExample.root, contentState.exampleViewVisibility)
        (recyclerView.adapter as SiteCreationDomainsAdapter).update(contentState.items)

        if (contentState.items.isNotEmpty()) {
            view?.announceForAccessibility(getString(R.string.suggestions_updated_content_description))
        }
    }

    private fun updateTitleVisibility(visible: Boolean) {
        val actionBar = (requireActivity() as? AppCompatActivity)?.supportActionBar
        actionBar?.setDisplayShowTitleEnabled(displayUtils.isLandscapeBySize() || visible)
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // we need to set the `onTextChanged` after the viewState has been restored otherwise the viewModel.updateQuery
        // is called when the system sets the restored value to the EditText which results in an unnecessary request
        searchInputWithHeader?.onTextChanged = { viewModel.updateQuery(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchInputWithHeader = null
        binding = null
    }

    companion object {
        const val TAG = "site_creation_domains_fragment_tag"

        fun newInstance(screenTitle: String): SiteCreationDomainsFragment {
            val fragment = SiteCreationDomainsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
