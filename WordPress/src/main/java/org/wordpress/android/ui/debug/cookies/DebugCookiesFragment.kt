package org.wordpress.android.ui.debug.cookies

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DebugCookiesFragmentBinding
import javax.inject.Inject

class DebugCookiesFragment : Fragment(R.layout.debug_cookies_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DebugCookiesViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(DebugCookiesViewModel::class.java)

        with(DebugCookiesFragmentBinding.bind(view)) {
            setupToolbar()
            setupViews()
            setupObservers()
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

        setCookieButton.setOnClickListener {
            viewModel.setCookie(
                hostInput.text.toString(),
                nameInput.text.toString(),
                valueInput.text.toString()
            )
        }
    }

    private fun DebugCookiesFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            (recyclerView.adapter as? DebugCookiesAdapter)?.submitList(uiState.items)
            uiState.hostInputText?.let { hostInput.setTextAndMoveCursor(it) }
            uiState.nameInputText?.let { nameInput.setTextAndMoveCursor(it) }
            uiState.valueInputText?.let { valueInput.setTextAndMoveCursor(it) }
        }
    }

    private fun EditText.setTextAndMoveCursor(text: String) {
        this.setText(text)
        this.setSelection(text.length)
    }

    companion object {
        fun newInstance() = DebugCookiesFragment()
    }
}
