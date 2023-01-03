@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.reader.subfilter

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.HiddenEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.SITE
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.ItemType.TAG
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.adapters.SubfilterListAdapter
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import org.wordpress.android.widgets.WPTextView
import java.lang.ref.WeakReference
import javax.inject.Inject

class SubfilterPageFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var statsUtils: StatsUtils

    private lateinit var subFilterViewModel: SubFilterViewModel
    private lateinit var viewModel: SubfilterPageViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var title: WPTextView
    private lateinit var actionButton: Button

    companion object {
        const val CATEGORY_KEY = "category_key"
        const val SUBFILTER_VIEW_MODEL_KEY = "subfilter_view_model_key"

        fun newInstance(category: SubfilterCategory, subfilterViewModelKey: String): SubfilterPageFragment {
            val fragment = SubfilterPageFragment()
            val bundle = Bundle()
            bundle.putSerializable(CATEGORY_KEY, category)
            bundle.putString(SUBFILTER_VIEW_MODEL_KEY, subfilterViewModelKey)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.subfilter_page_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val category = requireArguments().getSerializable(CATEGORY_KEY) as SubfilterCategory
        val subfilterVmKey = requireArguments().getString(SUBFILTER_VIEW_MODEL_KEY)!!

        viewModel = ViewModelProvider(this, viewModelFactory).get(SubfilterPageViewModel::class.java)
        viewModel.start(category)

        recyclerView = view.findViewById(R.id.content_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = SubfilterListAdapter(uiHelpers, statsUtils, seenUnseenWithCounterFeatureConfig)

        emptyStateContainer = view.findViewById(R.id.empty_state_container)
        title = emptyStateContainer.findViewById(R.id.title)
        actionButton = emptyStateContainer.findViewById(R.id.action_button)

        subFilterViewModel = ViewModelProvider(
            requireParentFragment().parentFragment as ViewModelStoreOwner,
            viewModelFactory
        ).get(subfilterVmKey, SubFilterViewModel::class.java)

        subFilterViewModel.subFilters.observe(viewLifecycleOwner, Observer {
            (recyclerView.adapter as? SubfilterListAdapter)?.let { adapter ->
                var items = it?.filter { it.type == category.type } ?: listOf()

                val currentFilter = subFilterViewModel.getCurrentSubfilterValue()

                if (items.isNotEmpty() && (currentFilter is Site || currentFilter is Tag)) {
                    items = items.map {
                        it.isSelected = it.isSameItem(currentFilter)
                        it
                    }
                }

                viewModel.onSubFiltersChanged(items.isEmpty())
                adapter.update(items)
                subFilterViewModel.onSubfilterPageUpdated(category, items.size)
            }
        })

        viewModel.emptyState.observe(viewLifecycleOwner, Observer { uiState ->
            if (isAdded) {
                when (uiState) {
                    HiddenEmptyUiState -> emptyStateContainer.visibility = View.GONE
                    is VisibleEmptyUiState -> {
                        emptyStateContainer.visibility = View.VISIBLE
                        title.setText(uiState.title.stringRes)
                        actionButton.setText(uiState.buttonText.stringRes)
                        actionButton.setOnClickListener {
                            subFilterViewModel.onBottomSheetActionClicked(uiState.action)
                        }
                    }
                }
            }
        })
    }

    fun setNestedScrollBehavior(enable: Boolean) {
        if (!isAdded) return

        recyclerView.isNestedScrollingEnabled = enable
    }
}

@Suppress("DEPRECATION")
class SubfilterPagerAdapter(
    val context: Context,
    val fm: FragmentManager,
    val subfilterViewModelKey: String,
    categories: List<SubfilterCategory>
) : FragmentPagerAdapter(fm) {
    private val filterCategory = categories
    private val fragments = mutableMapOf<SubfilterCategory, WeakReference<SubfilterPageFragment>>()

    override fun getCount(): Int = filterCategory.size

    override fun getItem(position: Int): Fragment {
        val fragment = SubfilterPageFragment.newInstance(filterCategory[position], subfilterViewModelKey)
        fragments[filterCategory[position]] = WeakReference(fragment)
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(filterCategory[position].titleRes)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        for (i in 0 until fragments.size) {
            val fragment = fragments[filterCategory[i]]?.get()
            fragment?.setNestedScrollBehavior(i == position)
        }
        container.requestLayout()
    }
}

enum class SubfilterCategory(@StringRes val titleRes: Int, val type: ItemType) : Parcelable {
    SITES(R.string.reader_filter_sites_title, SITE),
    TAGS(R.string.reader_filter_tags_title, TAG);

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Creator<SubfilterCategory> {
        override fun createFromParcel(parcel: Parcel): SubfilterCategory {
            return values()[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<SubfilterCategory?> {
            return arrayOfNulls(size)
        }
    }
}
