package org.wordpress.android.ui.domains

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainSuggestionsFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.domains.DomainRegistrationActivity.Companion.DOMAIN_REGISTRATION_PURPOSE_KEY
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.viewmodel.domains.DomainSuggestionsViewModel
import javax.inject.Inject

class DomainSuggestionsFragment : Fragment(R.layout.domain_suggestions_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var mainViewModel: DomainRegistrationMainViewModel
    private lateinit var viewModel: DomainSuggestionsViewModel

    companion object {
        const val TAG = "DOMAIN_SUGGESTIONS_FRAGMENT"
        fun newInstance(): DomainSuggestionsFragment {
            return DomainSuggestionsFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        mainViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(DomainRegistrationMainViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(DomainSuggestionsViewModel::class.java)

        with(DomainSuggestionsFragmentBinding.bind(view)) {
            val intent = requireActivity().intent
            val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            val domainRegistrationPurpose = intent.getSerializableExtra(DOMAIN_REGISTRATION_PURPOSE_KEY)
                    as DomainRegistrationPurpose

            setupViews()
            setupObservers()
            viewModel.start(site, domainRegistrationPurpose)
        }
    }

    private fun DomainSuggestionsFragmentBinding.setupViews() {
        domainSuggestionsList.layoutManager = LinearLayoutManager(activity)
        domainSuggestionsList.setEmptyView(actionableEmptyView)
        choseDomainButton.setOnClickListener {
            val selectedDomain = viewModel.selectedSuggestion.value

            mainViewModel.selectDomain(
                    DomainProductDetails(
                            selectedDomain!!.product_id,
                            selectedDomain.domain_name
                    )
            )
        }
        domainSuggestionKeywordInput.doAfterTextChanged { viewModel.updateSearchQuery(it.toString()) }
        domainSuggestionsList.adapter = DomainSuggestionsAdapter(::onDomainSuggestionSelected)
    }

    private fun DomainSuggestionsFragmentBinding.setupObservers() {
        viewModel.isIntroVisible.observe(viewLifecycleOwner) { introductionContainer.isVisible = it }
        viewModel.suggestionsLiveData.observe(viewLifecycleOwner) { listState ->
            val isLoading = listState is ListState.Loading<*>

            domainSuggestionsContainer.isInvisible = isLoading
            suggestionProgressBar.isVisible = isLoading
            suggestionSearchIcon.isGone = isLoading

            if (!isLoading) {
                reloadSuggestions(listState.data)
            }

            if (listState is ListState.Error<*>) {
                val errorMessage = listState.errorMessage.orEmpty().ifEmpty {
                    getString(R.string.domain_suggestions_fetch_error)
                }
                ToastUtils.showToast(context, errorMessage)
            }
        }
        viewModel.choseDomainButtonEnabledState.observe(viewLifecycleOwner) { choseDomainButton.isEnabled = it }
    }

    private fun DomainSuggestionsFragmentBinding.reloadSuggestions(domainSuggestions: List<DomainSuggestionResponse>) {
        val adapter = domainSuggestionsList.adapter as DomainSuggestionsAdapter
        adapter.selectedPosition = viewModel.selectedPosition.value ?: -1
        adapter.updateSuggestionsList(domainSuggestions)
    }

    private fun onDomainSuggestionSelected(domainSuggestion: DomainSuggestionResponse?, selectedPosition: Int) {
        viewModel.onDomainSuggestionsSelected(domainSuggestion, selectedPosition)
    }

    override fun onResume() {
        super.onResume()
        (activity as? ScrollableViewInitializedListener)?.onScrollableViewInitialized(R.id.domain_suggestions_list)
    }
}
