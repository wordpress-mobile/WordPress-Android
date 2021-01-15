package org.wordpress.android.ui.jetpack.restore.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import javax.inject.Inject

class RestoreDetailsFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var parentViewModel: RestoreViewModel
    private lateinit var viewModel: RestoreDetailsViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.restore_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        parentViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(RestoreViewModel::class.java)

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(RestoreDetailsViewModel::class.java)
    }

    companion object {
        const val TAG = "RESTORE_DETAILS_FRAGMENT"
        fun newInstance(bundle: Bundle?): RestoreDetailsFragment {
            return RestoreDetailsFragment().apply { arguments = bundle }
        }
    }
}
