package org.wordpress.android.ui.pages

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.pages_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.pages.PageStatus
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PageListViewModel
import org.wordpress.android.viewmodel.pages.PagesViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PageListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PageListViewModel
    private lateinit var linearLayoutManager: LinearLayoutManager

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        enum class Type(val text: Int) {
            PUBLISHED(R.string.pages_published),
            DRAFTS(R.string.pages_drafts),
            SCHEDULED(R.string.pages_scheduled),
            TRASH(R.string.pages_trashed);

            companion object {
                fun getType(position: Int): Type {
                    if (position >= values().size) {
                        throw IllegalArgumentException("Selected position $position is out of range of page list types")
                    }
                    return values()[position]
                }
            }
        }

        fun newInstance(type: Type): PageListFragment {
            val fragment = PageListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, type)
            fragment.arguments = bundle
            return fragment
        }
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
        val type = checkNotNull(arguments?.getSerializable(typeKey) as Type?)

        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullActivity, nonNullSite, type)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(listStateKey, linearLayoutManager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }

    private fun initializeViewModels(activity: FragmentActivity, site: SiteModel, type: Type) {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get<PageListViewModel>(type.name, PageListViewModel::class.java)

        setupObservers()

        val pagesViewModel = ViewModelProviders.of(activity, viewModelFactory).get(PagesViewModel::class.java)
        viewModel.start(site, getPageType(type), pagesViewModel)
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

    private fun setupObservers() {
        viewModel.pages.observe(this, Observer { data ->
            data?.let { setPages(data) }
        })
    }

    private fun getPageType(type: Type): PageStatus {
        return when (type) {
            Type.PUBLISHED -> PageStatus.PUBLISHED
            Type.DRAFTS -> PageStatus.DRAFT
            Type.SCHEDULED -> PageStatus.SCHEDULED
            else -> PageStatus.TRASHED
        }
    }

    private fun setPages(pages: List<PageItem>) {
        val adapter: PagesAdapter
        if (recyclerView.adapter == null) {
            adapter = PagesAdapter { action, pageItem -> viewModel.onAction(action, pageItem) }
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PagesAdapter
        }
        adapter.update(pages)
    }
}
