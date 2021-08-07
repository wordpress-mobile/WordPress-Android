package org.wordpress.android.ui.debug.cookies

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugCookiesFragmentBinding

class DebugCookiesFragment : DaggerFragment(R.layout.debug_cookies_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(DebugCookiesFragmentBinding.bind(view)) {
            setupToolbar()
            setupViews()
        }
    }

    private fun DebugCookiesFragmentBinding.setupToolbar() {
        with(requireActivity() as AppCompatActivity) {
            setSupportActionBar(toolbar)
            supportActionBar?.let {
                it.setHomeButtonEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun DebugCookiesFragmentBinding.setupViews() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = DebugCookiesAdapter()
        }
    }

    companion object {
        fun newInstance() = DebugCookiesFragment()
    }
}
