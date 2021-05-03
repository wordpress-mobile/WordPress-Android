package org.wordpress.android.ui.accounts.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import javax.inject.Inject

class JetpackLoginPrologueFragment : Fragment(R.layout.jetpack_login_prologue_screen) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: JetpackLoginPrologueViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this@JetpackLoginPrologueFragment, viewModelFactory).get(
                JetpackLoginPrologueViewModel::class.java
        )
        viewModel.start()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // important for accessibility - talkback
        activity?.setTitle(R.string.login_prologue_screen_title)
    }

    companion object {
        const val TAG = "jetpack_login_prologue_fragment_tag"
    }
}
