package org.wordpress.android.ui.reader

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModelProvider
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterPagerAdapter
import org.wordpress.android.util.extensions.getSerializableCompat
import javax.inject.Inject
import com.google.android.material.R as MaterialR

class SubfilterBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SubFilterViewModel

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.subfilter_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subfilterVmKey = requireArguments().getString(SUBFILTER_VIEW_MODEL_KEY)
        val bottomSheetTitle = requireArguments().getCharSequence(SUBFILTER_TITLE_KEY)
        val category = requireArguments().getSerializableCompat<SubfilterCategory>(SUBFILTER_CATEGORY_KEY)

        if (subfilterVmKey == null || category == null || bottomSheetTitle == null) {
            dismiss()
            return
        }

        viewModel = SubFilterViewModelProvider.getSubFilterViewModelForKey(this, subfilterVmKey)

        // TODO remove the pager and support only one category
        val pager = view.findViewById<ViewPager>(R.id.view_pager)
        val titleContainer = view.findViewById<View>(R.id.title_container)
        val title = view.findViewById<TextView>(R.id.title)
        val editSubscriptions = view.findViewById<View>(R.id.manage_subscriptions)
        title.text = bottomSheetTitle
        pager.adapter = SubfilterPagerAdapter(
            requireActivity(),
            childFragmentManager,
            subfilterVmKey,
            listOf(category)
        )
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // NO OP
            }

            override fun onPageSelected(position: Int) {
                val page = (pager.adapter as SubfilterPagerAdapter).getPageTitle(position)
                viewModel.trackOnPageSelected(page.toString())
            }

            override fun onPageScrollStateChanged(state: Int) {
                // NO OP
            }
        })

        pager.currentItem = when (viewModel.getCurrentSubfilterValue()) {
            is Tag -> TAGS.ordinal
            else -> SITES.ordinal
        }

        editSubscriptions.setOnClickListener {
            val subsPageIndex = when (category) {
                SITES -> ReaderSubsActivity.TAB_IDX_FOLLOWED_BLOGS
                TAGS -> ReaderSubsActivity.TAB_IDX_FOLLOWED_TAGS
            }
            viewModel.onBottomSheetActionClicked(ActionType.OpenSubsAtPage(subsPageIndex))
        }

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                MaterialR.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val metrics = it.context.resources.displayMetrics
                behavior.peekHeight = metrics.heightPixels / 2
            }

            dialog?.setOnShowListener(null)
        }

        viewModel.isTitleContainerVisible.observe(viewLifecycleOwner) { isVisible ->
            titleContainer.isVisible = isVisible
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.onBottomSheetCancelled()
    }
}
