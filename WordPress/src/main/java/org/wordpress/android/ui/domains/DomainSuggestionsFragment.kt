package org.wordpress.android.ui.domains

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var mainViewModel: DomainRegistrationMainViewModel
    private lateinit var viewModel: DomainSuggestionsViewModel

    companion object {
        const val TAG = "DOMAIN_SUGGESTIONS_FRAGMENT"
        fun newInstance(): DomainSuggestionsFragment {
            return DomainSuggestionsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.domain_suggestions_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)

        mainViewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(DomainRegistrationMainViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(DomainSuggestionsViewModel::class.java)

        val nonNullIntent = checkNotNull(nonNullActivity.intent)
        val site = nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel

        setupViews()
        setupObservers()
        viewModel.start(site)
    }

    private fun setupViews() {
        domain_suggestions_list.layoutManager = LinearLayoutManager(
                activity,
                RecyclerView.VERTICAL,
                false
        )
        domain_suggestions_list.setEmptyView(actionableEmptyView)
        chose_domain_button.setOnClickListener {
            val selectedDomain = viewModel.selectedSuggestion.value

            mainViewModel.selectDomain(
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
        viewModel.isIntroVisible.observe(viewLifecycleOwner, Observer {
            it?.let { isIntroVisible ->
                introduction_container.visibility = if (isIntroVisible) View.VISIBLE else View.GONE
            }
        })
        viewModel.suggestionsLiveData.observe(viewLifecycleOwner, Observer { listState ->
            if (listState != null) {
                val isLoading = listState is ListState.Loading<*>

                domain_suggestions_container.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                suggestion_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
                suggestion_search_icon.visibility = if (isLoading) View.GONE else View.VISIBLE

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
            chose_domain_button.isEnabled = it ?: false
        })
    }

    private fun reloadSuggestions(domainSuggestions: List<DomainSuggestionResponse>) {
        val adapter = domain_suggestions_list.adapter as DomainSuggestionsAdapter
        adapter.selectedPosition = viewModel.selectedPosition.value ?: -1
        adapter.updateSuggestionsList(domainSuggestions)
    }

    private fun onDomainSuggestionSelected(
        domainSuggestion: DomainSuggestionResponse?,
        selectedPosition: Int
    ) {
        viewModel.onDomainSuggestionsSelected(domainSuggestion, selectedPosition)
    }
}
