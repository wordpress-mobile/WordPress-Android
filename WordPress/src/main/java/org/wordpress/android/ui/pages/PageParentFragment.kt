package org.wordpress.android.ui.pages

import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v4.app.Fragment
import org.wordpress.android.WordPress
import org.wordpress.android.viewmodel.pages.PagesViewModel
import javax.inject.Inject

class PageParentFragment : Fragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagesViewModel

    private val listStateKey = "list_state"

    companion object {
        fun newInstance(): PagesFragment {
            return PagesFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
        setHasOptionsMenu(true)
    }
}
