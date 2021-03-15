package org.wordpress.android.ui.domains

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainSuggestionsFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.ui.ScrollableViewInitializedListener
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

        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component().inject(this)

        mainViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(DomainRegistrationMainViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(DomainSuggestionsViewModel::class.java)
        with(DomainSuggestionsFragmentBinding.bind(view)) {
            val nonNullIntent = checkNotNull(nonNullActivity.intent)
            val site = nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel

            setupViews()
            setupObservers()
            viewModel.start(site)
        }
    }

    private fun DomainSuggestionsFragmentBinding.setupViews() {
        domainSuggestionsList.layoutManager = LinearLayoutManager(
                activity,
                RecyclerView.VERTICAL,
                false
        )
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
        domainSuggestionKeywordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(view: Editable?) {
                viewModel.updateSearchQuery(view.toString())
            }
        })
        val adapter = DomainSuggestionsAdapter(this@DomainSuggestionsFragment::onDomainSuggestionSelected)
        domainSuggestionsList.adapter = adapter
    }

    private fun DomainSuggestionsFragmentBinding.setupObservers() {
        viewModel.isIntroVisible.observe(viewLifecycleOwner, Observer {
            it?.let { isIntroVisible ->
                introductionContainer.visibility = if (isIntroVisible) View.VISIBLE else View.GONE
            }
        })
        viewModel.suggestionsLiveData.observe(viewLifecycleOwner, Observer { listState ->
            if (listState != null) {
                val isLoading = listState is ListState.Loading<*>

                domainSuggestionsContainer.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                suggestionProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                suggestionSearchIcon.visibility = if (isLoading) View.GONE else View.VISIBLE

                if (!isLoading) {
                    reloadSuggestions(listState.data)
                }

                if (listState is ListState.Error<*>) {
                    val errorMessage = if (TextUtils.isEmpty(listState.errorMessage)) {
                        getString(R.string.domain_suggestions_fetch_error)
                    } else {
                        listState.errorMessage
                    }
                    ToastUtils.showToast(context, errorMessage)
                }
            }
        })
        viewModel.choseDomainButtonEnabledState.observe(viewLifecycleOwner, Observer {
            choseDomainButton.isEnabled = it ?: false
        })
    }

    private fun DomainSuggestionsFragmentBinding.reloadSuggestions(domainSuggestions: List<DomainSuggestionResponse>) {
        val adapter = domainSuggestionsList.adapter as DomainSuggestionsAdapter
        adapter.selectedPosition = viewModel.selectedPosition.value ?: -1
        adapter.updateSuggestionsList(domainSuggestions)
    }

    private fun onDomainSuggestionSelected(
        domainSuggestion: DomainSuggestionResponse?,
        selectedPosition: Int
    ) {
        viewModel.onDomainSuggestionsSelected(domainSuggestion, selectedPosition)
    }

    override fun onResume() {
        super.onResume()
        if (activity is ScrollableViewInitializedListener) {
            (activity as ScrollableViewInitializedListener).onScrollableViewInitialized(R.id.domain_suggestions_list)
        }
    }
}
