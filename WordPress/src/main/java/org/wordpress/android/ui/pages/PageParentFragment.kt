package org.wordpress.android.ui.pages

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.pages_list_fragment.*
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.pages.PageParentViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject
import javax.inject.Named

class PageParentFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @field:[Inject Named(UI_SCOPE)] lateinit var uiScope: CoroutineScope
    private lateinit var viewModel: PageParentViewModel

    private val listStateKey = "list_state"

    private var linearLayoutManager: LinearLayoutManager? = null
    private var saveButton: MenuItem? = null

    private var pageId: Long? = null

    companion object {
        fun newInstance(): PageParentFragment {
            return PageParentFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
            return true
        } else if (item.itemId == R.id.save_parent) {
            viewModel.onSaveButtonTapped()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun returnParentChoiceAndExit() {
        val result = Intent()
        result.putExtra(EXTRA_PAGE_REMOTE_ID_KEY, pageId)
        result.putExtra(EXTRA_PAGE_PARENT_ID_KEY, viewModel.currentParent.id)
        activity?.setResult(Activity.RESULT_OK, result)
        activity?.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.page_parent_menu, menu)

        saveButton = menu.findItem(R.id.save_parent)
        viewModel.isSaveButtonVisible.value?.let { saveButton?.isVisible = it }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageId = activity?.intent?.getLongExtra(EXTRA_PAGE_REMOTE_ID_KEY, 0)

        val nonNullPageId = checkNotNull(pageId)
        val nonNullActivity = checkNotNull(activity)

        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        initializeViews(savedInstanceState)
        initializeViewModels(nonNullPageId, savedInstanceState == null)
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

    private fun initializeViewModels(pageId: Long, isFirstStart: Boolean) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PageParentViewModel::class.java)

        setupObservers()

        if (isFirstStart) {
            val site = activity?.intent?.getSerializableExtra(WordPress.SITE) as SiteModel?
            val nonNullSite = checkNotNull(site)
            viewModel.start(nonNullSite, pageId)
        }
    }

    private fun setupObservers() {
        viewModel.pages.observe(this, Observer { pages ->
            pages?.let { setPages(pages) }
        })

        viewModel.isSaveButtonVisible.observe(this, Observer { isVisible ->
            isVisible?.let { saveButton?.isVisible = isVisible }
        })

        viewModel.saveParent.observe(this, Observer {
            returnParentChoiceAndExit()
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        linearLayoutManager?.let { outState.putParcelable(listStateKey, it.onSaveInstanceState()) }
        super.onSaveInstanceState(outState)
    }

    private fun setPages(pages: List<PageItem>) {
        val adapter: PageParentAdapter
        if (recyclerView.adapter == null) {
            adapter = PageParentAdapter({ page -> viewModel.onParentSelected(page) }, uiScope)
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as PageParentAdapter
        }
        adapter.update(pages)
    }
}
