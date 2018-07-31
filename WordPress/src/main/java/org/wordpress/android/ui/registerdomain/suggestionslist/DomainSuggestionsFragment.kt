package org.wordpress.android.ui.registerdomain.suggestionslist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.domain_suggestions_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.viewmodel.registerdomain.DomainSuggestionsViewModel
import javax.inject.Inject

class DomainSuggestionsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainSuggestionsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.domain_suggestions_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity?.application as WordPress).component()?.inject(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DomainSuggestionsViewModel::class.java)
        val site = if (savedInstanceState == null) {
            activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        setupViews()
        setupObservers()
        viewModel.start(site)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    private fun setupViews() {
        domainSuggestionsList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        domainSuggestionsList.setEmptyView(actionableEmptyView)
        chooseDomainButton.setOnClickListener {
            ToastUtils.showToast(activity, "Still under development.")
        }
    }

    private fun setupObservers() {
        viewModel.suggestionsLiveData.observe(this, Observer {
            reloadSuggestions(it?.data ?: emptyList())
        })

        viewModel.selectedSuggestion.observe(this, Observer {
            chooseDomainButton.isEnabled = it is DomainSuggestionResponse
        })
    }

    private fun reloadSuggestions(domainSuggestions: List<DomainSuggestionResponse>) {
        val adapter: DomainSuggestionsAdapter
        if (domainSuggestionsList.adapter == null) {
            adapter = DomainSuggestionsAdapter(this::onDomainSuggestionSelected)
            domainSuggestionsList.adapter = adapter
        } else {
            adapter = domainSuggestionsList.adapter as DomainSuggestionsAdapter
        }
        adapter.updateSuggestionsList(domainSuggestions)
    }

    private fun onDomainSuggestionSelected(domainSuggestion: DomainSuggestionResponse) {
        viewModel.onDomainSuggestionsSelected(domainSuggestion)
    }
}
