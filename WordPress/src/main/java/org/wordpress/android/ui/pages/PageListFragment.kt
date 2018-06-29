package org.wordpress.android.ui.pages

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
import javax.inject.Inject

class PageListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PageListViewModel

    private val listStateKey = "list_state"

    companion object {
        const val fragmentKey = "fragment_key"
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

        fun newInstance(key: String, type: Type): PageListFragment {
            val fragment = PageListFragment()
            val bundle = Bundle()
            bundle.putString(fragmentKey, key)
            bundle.putSerializable(typeKey, type)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_list_fragment, container, false)
    }

    override fun onDestroyView() {
        viewModel.stop()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView(savedInstanceState)

        initViewModel(savedInstanceState)
    }

    private fun initRecyclerView(savedInstanceState: Bundle?) {
        val linearLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        savedInstanceState?.getParcelable<Parcelable>(listStateKey)?.let {
            linearLayoutManager.onRestoreInstanceState(it)
        }
        recyclerView.layoutManager = linearLayoutManager

        (activity?.application as WordPress).component()?.inject(this)
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        val key = arguments?.getString(fragmentKey)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<PageListViewModel>(checkNotNull(key), PageListViewModel::class.java)

        val site = (savedInstanceState?.getSerializable(WordPress.SITE)
                ?: activity?.intent?.getSerializableExtra(WordPress.SITE)) as SiteModel

        viewModel.start(site)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(listStateKey, recyclerView.layoutManager.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }
}
