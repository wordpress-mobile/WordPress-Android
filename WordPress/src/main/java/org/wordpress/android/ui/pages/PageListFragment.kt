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
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.pages_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.pages.PageListViewModel
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PagesViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PageListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var imageManager: ImageManager
    @Inject internal lateinit var uiHelper: UiHelpers
    private lateinit var viewModel: PageListViewModel
    private var linearLayoutManager: LinearLayoutManager? = null

    private val listStateKey = "list_state"

    companion object {
        private const val typeKey = "type_key"

        fun newInstance(listType: PageListType): PageListFragment {
            val fragment = PageListFragment()
            val bundle = Bundle()
            bundle.putSerializable(typeKey, listType)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
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
        val pagesViewModel = ViewModelProviders.of(activity, viewModelFactory).get(PagesViewModel::class.java)

        val listType = arguments?.getSerializable(typeKey) as PageListType
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(listType.name, PageListViewModel::class.java)

        viewModel.start(listType, pagesViewModel)

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
        viewModel.pages.observe(viewLifecycleOwner, Observer { data ->
            data?.let { setPages(data.first, data.second, data.third) }
        })

        viewModel.scrollToPosition.observe(viewLifecycleOwner, Observer { position ->
            position?.let {
                val smoothScroller = object : LinearSmoothScroller(context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }.apply { targetPosition = position }
                recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
            }
        })
    }

    private fun setPages(pages: List<PageItem>, isSitePhotonCapable: Boolean, isSitePrivateAt: Boolean) {
        val adapter: PageListAdapter
        if (recyclerView.adapter == null) {
            adapter = PageListAdapter(
                    { action, page -> viewModel.onMenuAction(action, page) },
                    { page -> viewModel.onItemTapped(page) },
                    { viewModel.onEmptyListNewPageButtonTapped() },
                    isSitePhotonCapable,
                    isSitePrivateAt,
                    imageManager,
                    uiHelper
            )
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PageListAdapter
        }
        adapter.update(pages)
    }

    fun scrollToPage(localPageId: Int) {
        viewModel.onScrollToPageRequested(localPageId)
    }
}
