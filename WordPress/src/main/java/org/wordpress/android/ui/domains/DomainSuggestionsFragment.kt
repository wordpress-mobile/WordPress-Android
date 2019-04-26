package org.wordpress.android.ui.domains

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.domain_suggestions_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.viewmodel.domains.DomainSuggestionsViewModel
import javax.inject.Inject

class DomainSuggestionsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainSuggestionsViewModel

    companion object {
        fun newInstance(): DomainSuggestionsFragment {
            return DomainSuggestionsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.domain_suggestions_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DomainSuggestionsViewModel::class.java)

        val nonNullIntent = checkNotNull(nonNullActivity.intent)
        val site = nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel

        setupViews()
        setupObservers()
        viewModel.start(site)
    }

    private fun setupViews() {
        domain_suggestions_list.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        domain_suggestions_list.setEmptyView(actionableEmptyView)
        chose_domain_button.setOnClickListener {
            val selectedDomain = viewModel.selectedSuggestion.value

            (activity as DomainRegistrationActivity).onDomainSelected(
                    DomainProductDetails(
                            selectedDomain!!.product_id,
                            selectedDomain.domain_name
                    )
            )
        }
        domain_suggestion_keyword_input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(view: Editable?) {
                viewModel.updateSearchQuery(view.toString())
            }
        })
        val adapter = DomainSuggestionsAdapter(this::onDomainSuggestionSelected)
        domain_suggestions_list.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.suggestionsLiveData.observe(this, Observer { listState ->
            if (listState != null) {
                val isLoading = listState is ListState.Loading<*>

                domain_suggestions_container.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                suggestion_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE

                if (!isLoading) {
                    reloadSuggestions(listState.data)
                }

                if (listState is ListState.Error<*>) {
                    val errorMessage = if (TextUtils.isEmpty(listState.errorMessage)) {
                        getString(R.string.suggestion_fetch_error)
                    } else {
                        listState.errorMessage
                    }
                    ToastUtils.showToast(context, errorMessage)
                }
            }
        })
        viewModel.choseDomainButtonEnabledState.observe(this, Observer {
            chose_domain_button.isEnabled = it ?: false
        })
    }

    private fun reloadSuggestions(domainSuggestions: List<DomainSuggestionResponse>) {
        val adapter = domain_suggestions_list.adapter as DomainSuggestionsAdapter
        adapter.selectedPosition = viewModel.selectedPosition.value ?: -1
        adapter.updateSuggestionsList(domainSuggestions)
    }

    private fun onDomainSuggestionSelected(domainSuggestion: DomainSuggestionResponse?, selectedPosition: Int) {
        viewModel.onDomainSuggestionsSelected(domainSuggestion, selectedPosition)
    }

    interface OnDomainSelectedListener {
        fun onDomainSelected(domainProductDetails: DomainProductDetails)
    }
}
