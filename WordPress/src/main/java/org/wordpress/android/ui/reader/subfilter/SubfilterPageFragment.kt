@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.reader.subfilter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
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
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.widgets.WPTextView
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class SubfilterPageFragment : Fragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var statsUtils: StatsUtils

    private lateinit var subFilterViewModel: SubFilterViewModel
    private lateinit var viewModel: SubfilterPageViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var title: WPTextView
    private lateinit var text: WPTextView
    private lateinit var primaryButton: Button
    private lateinit var secondaryButton: Button

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

        val category = requireNotNull(arguments?.getSerializableCompat<SubfilterCategory>(CATEGORY_KEY))
        val subfilterVmKey = requireArguments().getString(SUBFILTER_VIEW_MODEL_KEY)!!

        viewModel = ViewModelProvider(this, viewModelFactory)[SubfilterPageViewModel::class.java]
        viewModel.start(category)

        recyclerView = view.findViewById(R.id.content_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter =
            SubfilterListAdapter(uiHelpers, statsUtils, imageManager, seenUnseenWithCounterFeatureConfig)

        emptyStateContainer = view.findViewById(R.id.empty_state_container)
        title = emptyStateContainer.findViewById(R.id.title)
        text = emptyStateContainer.findViewById(R.id.text)
        primaryButton = emptyStateContainer.findViewById(R.id.action_button_primary)
        secondaryButton = emptyStateContainer.findViewById(R.id.action_button_secondary)

        subFilterViewModel = SubFilterViewModelProvider.getSubFilterViewModelForKey(this, subfilterVmKey)

        subFilterViewModel.subFilters.observe(viewLifecycleOwner) {
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
            }
        }

        viewModel.emptyState.observe(viewLifecycleOwner) { uiState ->
            if (isAdded) {
                when (uiState) {
                    HiddenEmptyUiState -> hideEmptyUi()
                    is VisibleEmptyUiState -> showEmptyUi(uiState)
                }
            }
        }
    }

    private fun hideEmptyUi() {
        emptyStateContainer.visibility = View.GONE
        subFilterViewModel.setTitleContainerVisibility(isVisible = true)
    }

    private fun showEmptyUi(uiState: VisibleEmptyUiState) {
        emptyStateContainer.visibility = View.VISIBLE
        subFilterViewModel.setTitleContainerVisibility(isVisible = false)

        if (uiState.title == null) {
            title.visibility = View.GONE
        } else {
            title.visibility = View.VISIBLE
            title.text = uiHelpers.getTextOfUiString(requireContext(), uiState.title)
        }

        text.text = uiHelpers.getTextOfUiString(requireContext(), uiState.text)

        if (uiState.primaryButton == null) {
            primaryButton.visibility = View.GONE
        } else {
            primaryButton.visibility = View.VISIBLE
            primaryButton.text = uiHelpers.getTextOfUiString(requireContext(), uiState.primaryButton.text)
            primaryButton.setOnClickListener {
                subFilterViewModel.onBottomSheetActionClicked(uiState.primaryButton.action)
            }
        }

        if (uiState.secondaryButton == null) {
            secondaryButton.visibility = View.GONE
        } else {
            secondaryButton.visibility = View.VISIBLE
            secondaryButton.text = uiHelpers.getTextOfUiString(requireContext(), uiState.secondaryButton.text)
            secondaryButton.setOnClickListener {
                subFilterViewModel.onBottomSheetActionClicked(uiState.secondaryButton.action)
            }
        }
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
    private val subfilterViewModelKey: String,
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

    override fun getPageTitle(position: Int): CharSequence {
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

enum class SubfilterCategory(@StringRes val titleRes: Int, val type: ItemType) {
    SITES(R.string.reader_filter_by_blog_title, SITE),
    TAGS(R.string.reader_filter_by_tag_title, TAG);
}
