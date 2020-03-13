package org.wordpress.android.ui.pages

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.pages_list_fragment.*
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PageParentSearchViewModel
import org.wordpress.android.viewmodel.pages.PageParentViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject
import javax.inject.Named

class PageParentSearchFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PageParentSearchViewModel
    @field:[Inject Named(UI_SCOPE)] lateinit var uiScope: CoroutineScope
    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        fun newInstance(): PageParentSearchFragment {
            return PageParentSearchFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }
        super.onSaveInstanceState(outState)
    }

    private fun initializeViewModels(activity: FragmentActivity) {
        val pageParentViewModel = ViewModelProviders.of(activity, viewModelFactory)
                .get(PageParentViewModel::class.java)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PageParentSearchViewModel::class.java)
        viewModel.start(pageParentViewModel)

        setupObservers()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))
    }

    private fun setupObservers() {
        viewModel.searchResult.observe(this, Observer { data ->
            data?.let { setSearchResult(data) }
        })
    }

    private fun setSearchResult(pages: List<PageItem>) {
        val adapter: PageParentSearchAdapter
        if (recyclerView.adapter == null) {
            adapter = PageParentSearchAdapter(
                    { page -> viewModel.onParentSelected(page) }, uiScope)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PageParentSearchAdapter
        }
        adapter.update(pages)
    }
}
