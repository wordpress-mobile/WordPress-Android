package org.wordpress.android.ui.support

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SupportFormFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.SiteUtils
import javax.inject.Inject

class SupportFormFragment : Fragment(R.layout.support_form_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SupportFormViewModel

    @Inject lateinit var siteStore: SiteStore

    private val selectedSiteFromExtras by lazy { activity?.intent?.extras?.get(WordPress.SITE) as SiteModel? }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(SupportFormViewModel::class.java)

        with(SupportFormFragmentBinding.bind(view)) {
            setupToolbar()
            setupViews()
        }
    }

    private fun SupportFormFragmentBinding.setupToolbar() {
        with(requireActivity() as AppCompatActivity) {
            setSupportActionBar(toolbar)
            supportActionBar?.let {
                it.setHomeButtonEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun SupportFormFragmentBinding.setupViews() {
        val reasonForContactItems = resources.getStringArray(R.array.support_form_reason_for_contact_array)
        val reasonForContactAdapter = ArrayAdapter(
                requireContext(),
                R.layout.support_form_simple_list_item,
                reasonForContactItems
        )
        reasonForContactTextView.setAdapter(reasonForContactAdapter)
        reasonForContactTextView.setText(reasonForContactItems.first(), false)

        val feelingItems = resources.getStringArray(R.array.support_form_feeling_array)
        val feelingAdapter = ArrayAdapter(requireContext(), R.layout.support_form_simple_list_item, feelingItems)
        feelingTextView.setAdapter(feelingAdapter)
        feelingTextView.setText(feelingItems.first(), false)

        val allSites = siteStore.sites
        val siteItems = allSites.map { SiteUtils.getHomeURLOrHostName(it) }
        val siteAdapter = ArrayAdapter(requireContext(), R.layout.support_form_simple_list_item, siteItems)
        siteTextView.setAdapter(siteAdapter)
        siteTextView.setText(SiteUtils.getHomeURLOrHostName(selectedSiteFromExtras ?: allSites.first()), false)

        messageEditText.doAfterTextChanged {
            sendMessageButton.isEnabled = it?.isNotBlank() == true
        }

        sendMessageButton.setOnClickListener {
            showProgress(true)

            val message = "How you can help: ${reasonForContactTextView.text}\n" +
                    "How I feel: ${feelingTextView.text}\n" +
                    "Site I need help with: ${siteTextView.text}\n\n" +
                    messageEditText.text

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun SupportFormFragmentBinding.showProgress(show: Boolean) {
        sendMessageButtonProgress.isVisible = show
        reasonForContactInputLayout.isEnabled = !show
        feelingInputLayout.isEnabled = !show
        siteInputLayout.isEnabled = !show
        messageInputLayout.isEnabled = !show
        sendMessageButton.isEnabled = !show
    }
}
