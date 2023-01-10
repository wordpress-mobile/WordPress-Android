package org.wordpress.android.ui.pages

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PagesListFragmentBinding
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PageParentSearchViewModel
import org.wordpress.android.viewmodel.pages.PageParentViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class PageParentSearchFragment : Fragment(R.layout.pages_list_fragment), CoroutineScope {
    protected var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PageParentSearchViewModel
    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        fun newInstance(): PageParentSearchFragment {
            return PageParentSearchFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nonNullActivity = requireActivity()
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)
        with(PagesListFragmentBinding.bind(view)) {
            initializeViews(savedInstanceState)
            initializeViewModels(nonNullActivity)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let {
            outState.putParcelable(listStateKey, it.onSaveInstanceState())
        }
        super.onSaveInstanceState(outState)
    }

    private fun PagesListFragmentBinding.initializeViewModels(activity: FragmentActivity) {
        val pageParentViewModel = ViewModelProvider(activity, viewModelFactory)
            .get(PageParentViewModel::class.java)

        viewModel = ViewModelProvider(this@PageParentSearchFragment, viewModelFactory)
            .get(PageParentSearchViewModel::class.java)
        viewModel.start(pageParentViewModel)

        setupObservers()
    }

    private fun PagesListFragmentBinding.initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))
    }

    private fun PagesListFragmentBinding.setupObservers() {
        viewModel.searchResult.observe(viewLifecycleOwner, Observer { data ->
            data?.let { setSearchResult(data) }
        })
    }

    private fun PagesListFragmentBinding.setSearchResult(pages: List<PageItem>) {
        val adapter: PageParentSearchAdapter
        if (recyclerView.adapter == null) {
            adapter = PageParentSearchAdapter(
                { page -> viewModel.onParentSelected(page) }, this@PageParentSearchFragment
            )
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PageParentSearchAdapter
        }
        adapter.update(pages)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
