package org.wordpress.android.ui.domainregister.suggestionslist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.domain_suggestions_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.helpers.Debouncer
import org.wordpress.android.viewmodel.domainregister.DomainSuggestionsViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DomainSuggestionsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainSuggestionsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.domain_suggestions_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkNotNull((activity?.application as WordPress).component())
        (activity?.application as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(DomainSuggestionsViewModel::class.java)
        val site = activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel

        setupViews()
        setupObservers()
        viewModel.start(site)
    }

    private fun setupViews() {
        domainSuggestionsList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        domainSuggestionsList.setEmptyView(actionableEmptyView)
        chooseDomainButton.setOnClickListener {
            // TODO Implement Activity navigation
            ToastUtils.showToast(activity, "Still under development.")
        }
        domainSearchEditText.addTextChangedListener(object : TextWatcher {
            private val debouncer = Debouncer()

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(view: Editable?) {
                debouncer.debounce(Void::class.java, {
                    viewModel.updateSearchQuery(view.toString())
                }, GET_SUGGESTIONS_INTERVAL_MS, TimeUnit.MILLISECONDS)
            }
        })
        val adapter = DomainSuggestionsAdapter(this::onDomainSuggestionSelected)
        domainSuggestionsList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.isLoadingInProgress.observe(this, Observer {
            domainSuggestionsListContainer.visibility = if (it == true) View.INVISIBLE else View.VISIBLE
            suggestionsProgressBar.visibility = if (it == true) View.VISIBLE else View.GONE
        })
        viewModel.suggestionsLiveData.observe(this, Observer {
            if (it != null) {
                reloadSuggestions(it)
            }
        })
        viewModel.shouldEnableChooseDomain.observe(this, Observer {
            chooseDomainButton.isEnabled = it ?: false
        })
    }

    private fun reloadSuggestions(domainSuggestions: List<DomainSuggestionResponse>) {
        val adapter = domainSuggestionsList.adapter as DomainSuggestionsAdapter
        adapter.selectedPosition = viewModel.selectedPosition.value ?: -1
        adapter.updateSuggestionsList(domainSuggestions)
    }

    private fun onDomainSuggestionSelected(domainSuggestion: DomainSuggestionResponse?, selectedPosition: Int) {
        viewModel.onDomainSuggestionsSelected(domainSuggestion, selectedPosition)
    }

    companion object {
        private const val GET_SUGGESTIONS_INTERVAL_MS = 250L
    }
}
