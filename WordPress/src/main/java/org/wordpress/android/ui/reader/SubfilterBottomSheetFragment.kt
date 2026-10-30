package org.wordpress.android.ui.reader

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.WordPress
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel
import org.wordpress.android.ui.reader.views.compose.subfilter.SubfilterSheet
import org.wordpress.android.ui.stats.refresh.utils.ONE_THOUSAND
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import org.wordpress.android.util.extensions.getSerializableCompat
import javax.inject.Inject

class SubfilterBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Inject
    lateinit var statsUtils: StatsUtils

    private var viewModel: SubFilterViewModel? = null
    private var pageViewModel: SubfilterPageViewModel? = null
    private var category: SubfilterCategory? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args = requireArguments()
        val subfilterVmKey = args.getString(SUBFILTER_VIEW_MODEL_KEY)
        val title = args.getCharSequence(SUBFILTER_TITLE_KEY)?.toString()
        val category = args.getSerializableCompat<SubfilterCategory>(SUBFILTER_CATEGORY_KEY)
            .also { this.category = it }

        if (subfilterVmKey == null || category == null || title == null) {
            dismiss()
            return null
        }

        val viewModel = SubFilterViewModelProvider.getSubFilterViewModelForKey(this, subfilterVmKey)
            .also { viewModel = it }
        val pageViewModel = ViewModelProvider(this, viewModelFactory)[SubfilterPageViewModel::class.java]
            .also { pageViewModel = it }
        pageViewModel.start(category)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val subFilters by viewModel.subFilters.observeAsState()
                val currentSubfilter by viewModel.currentSubFilter.observeAsState()
                val emptyState by pageViewModel.emptyState.observeAsState()
                AppThemeM3 {
                    SubfilterSheet(
                        title = title,
                        items = subFilters.orEmpty().filter { it.type == category.type },
                        currentSubfilter = currentSubfilter,
                        emptyState = emptyState as? VisibleEmptyUiState,
                        unseenCountText = ::unseenCountText,
                        onManageClick = { onManageClicked(viewModel, category) },
                        onEmptyStateAction = viewModel::onBottomSheetActionClicked
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.peekHeight = resources.displayMetrics.heightPixels / 2

        // the page view model decides whether the list is empty, and what to show instead
        viewModel?.subFilters?.observe(viewLifecycleOwner) { subFilters ->
            pageViewModel?.onSubFiltersChanged(subFilters.orEmpty().none { it.type == category?.type })
        }
    }

    private fun unseenCountText(site: Site): String? =
        if (seenUnseenWithCounterFeatureConfig.isEnabled() && site.showUnseenCount) {
            statsUtils.toFormattedString(site.unseenCount, ONE_THOUSAND)
        } else {
            null
        }

    private fun onManageClicked(viewModel: SubFilterViewModel, category: SubfilterCategory) {
        val subsPageIndex = when (category) {
            SITES -> ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS
            TAGS -> ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS
        }
        viewModel.onBottomSheetActionClicked(ActionType.OpenSubsAtPage(subsPageIndex))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel?.onBottomSheetCancelled()
    }

    companion object {
        const val SUBFILTER_VIEW_MODEL_KEY = "subfilter_view_model_key"
        const val SUBFILTER_TITLE_KEY = "subfilter_title_key"
        const val SUBFILTER_CATEGORY_KEY = "subfilter_category_key"

        @JvmStatic
        fun newInstance(
            subfilterViewModelKey: String,
            category: SubfilterCategory,
            title: CharSequence
        ): SubfilterBottomSheetFragment {
            val fragment = SubfilterBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString(SUBFILTER_VIEW_MODEL_KEY, subfilterViewModelKey)
            bundle.putCharSequence(SUBFILTER_TITLE_KEY, title)
            bundle.putSerializable(SUBFILTER_CATEGORY_KEY, category)

            fragment.arguments = bundle
            return fragment
        }
    }
}
