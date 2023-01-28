package org.wordpress.android.ui.domains

import android.os.Bundle
import android.view.View
import androidx.core.text.parseAsHtml
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
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.domains.DomainRegistrationActivity.Companion.DOMAIN_REGISTRATION_PURPOSE_KEY
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class DomainSuggestionsFragment : Fragment(R.layout.domain_suggestions_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
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

        mainViewModel =
            ViewModelProvider(requireActivity(), viewModelFactory)[DomainRegistrationMainViewModel::class.java]

        viewModel = ViewModelProvider(this, viewModelFactory)[DomainSuggestionsViewModel::class.java]

        with(DomainSuggestionsFragmentBinding.bind(view)) {
            val intent = requireActivity().intent
            val site = intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE)
            val domainRegistrationPurpose =
                intent.getSerializableExtraCompat<DomainRegistrationPurpose>(DOMAIN_REGISTRATION_PURPOSE_KEY)

            setupViews()
            setupObservers()
            if (site != null && domainRegistrationPurpose != null) {
                viewModel.start(site, domainRegistrationPurpose)
            }
        }
    }

    private fun DomainSuggestionsFragmentBinding.setupViews() {
        domainSuggestionsList.layoutManager = LinearLayoutManager(activity)
        domainSuggestionsList.setEmptyView(actionableEmptyView)
        selectDomainButton.setOnClickListener { viewModel.onSelectDomainButtonClicked() }
        domainSuggestionKeywordInput.doAfterTextChanged { viewModel.updateSearchQuery(it.toString()) }
        domainSuggestionsList.adapter = DomainSuggestionsAdapter(viewModel::onDomainSuggestionSelected)
    }

    private fun DomainSuggestionsFragmentBinding.setupObservers() {
        viewModel.isIntroVisible.observe(viewLifecycleOwner) { introductionContainer.isVisible = it }
        viewModel.showRedirectMessage.observe(viewLifecycleOwner) {
            it?.let {
                introLine1.isVisible = false
                introLine2.isVisible = false

                redirectMessage.isVisible = true
                redirectDivider.isVisible = true
                redirectMessage.text = getString(R.string.domains_free_plan_get_your_domain_caption, it).parseAsHtml()
            }
        }
        viewModel.isButtonProgressBarVisible.observe(viewLifecycleOwner) { isVisible ->
            buttonProgressBar.isVisible = isVisible
            selectDomainButton.textScaleX = if (isVisible) 0f else 1f
            selectDomainButton.isClickable = !isVisible
            domainSuggestionKeywordInput.isEnabled = !isVisible
        }
        viewModel.suggestionsLiveData.observe(viewLifecycleOwner) { listState ->
            val isLoading = listState is ListState.Loading<*>

            domainSuggestionsContainer.isInvisible = isLoading
            suggestionSearchIcon.isVisible = !isLoading
            suggestionProgressBar.isVisible = isLoading

            if (!isLoading) {
                (domainSuggestionsList.adapter as DomainSuggestionsAdapter).submitList(listState.data)
            }

            if (listState is ListState.Error<*>) {
                val errorMessage = listState.errorMessage.orEmpty().ifEmpty {
                    getString(R.string.domain_suggestions_fetch_error)
                }
                ToastUtils.showToast(context, errorMessage)
            }
        }
        viewModel.selectDomainButtonEnabledState.observe(viewLifecycleOwner) { selectDomainButton.isEnabled = it }
        viewModel.onDomainSelected.observeEvent(viewLifecycleOwner, mainViewModel::selectDomain)
    }

    override fun onResume() {
        super.onResume()
        (activity as? ScrollableViewInitializedListener)?.onScrollableViewInitialized(R.id.domain_suggestions_list)
    }
}
