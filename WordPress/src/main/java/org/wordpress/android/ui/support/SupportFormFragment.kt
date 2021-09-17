package org.wordpress.android.ui.support

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SupportFormFragmentBinding
import javax.inject.Inject

class SupportFormFragment : Fragment(R.layout.support_form_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SupportFormViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(SupportFormViewModel::class.java)

        with(SupportFormFragmentBinding.bind(view)) {
            setupToolbar()
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
}
