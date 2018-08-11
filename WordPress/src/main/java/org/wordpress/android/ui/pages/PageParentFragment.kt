package org.wordpress.android.ui.pages

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.pages_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PageParentViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PageParentFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PageParentViewModel

    private val listStateKey = "list_state"

    private var linearLayoutManager: LinearLayoutManager? = null

    companion object {
        fun newInstance(): PageParentFragment {
            return PageParentFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val site = if (savedInstanceState == null) {
            activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
        }

        val nonNullActivity = checkNotNull(activity)
        val nonNullSite = checkNotNull(site)

        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullSite, savedInstanceState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))
    }

    private fun initializeViewModels(site: SiteModel, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PageParentViewModel::class.java)

        setupObservers()

        viewModel.start(site)
    }

    private fun setupObservers() {
        viewModel.pages.observe(this, Observer { pages ->
            pages?.let { setPages(pages) }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let { outState.putParcelable(listStateKey, it.onSaveInstanceState()) }
        super.onSaveInstanceState(outState)
    }

    private fun setPages(pages: List<PageItem>) {
        val adapter: PagesAdapter
        if (recyclerView.adapter == null) {
            adapter = PagesAdapter(onParentSelected = { page -> viewModel.onParentSelected(page) })
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PagesAdapter
        }
        adapter.update(pages)
    }
}
