package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.WPActivityUtils
import javax.inject.Inject

class JetpackLoginPrologueFragment : Fragment(R.layout.jetpack_login_prologue_screen) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: JetpackLoginPrologueViewModel
    private lateinit var loginPrologueListener: LoginPrologueListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is LoginPrologueListener) {
            throw RuntimeException("$context must implement LoginPrologueListener")
        }
        loginPrologueListener = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initSystemBars(view)
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

    override fun onDestroyView() {
        super.onDestroyView()
        resetSystemBars()
    }

    private fun initSystemBars(view: View) {
        // setting up a full screen flags for the decor view of this fragment,
        // that will work with transparent status bar
        WPActivityUtils.showFullScreen(view)
        activity?.let {
            WPActivityUtils.setLightStatusBar(it.window, false)
            WPActivityUtils.setLightNavigationBar(it.window, false)
        }
    }

    private fun resetSystemBars() {
        activity?.let {
            WPActivityUtils.setLightStatusBar(it.window, true)
            WPActivityUtils.setLightNavigationBar(it.window, true)
        }
    }

    companion object {
        const val TAG = "jetpack_login_prologue_fragment_tag"
    }
}
