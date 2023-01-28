package org.wordpress.android.ui.reader

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterPagerAdapter
import org.wordpress.android.util.extensions.getParcelableArrayListCompat
import javax.inject.Inject

class SubfilterBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SubFilterViewModel

    companion object {
        const val SUBFILTER_VIEW_MODEL_KEY = "subfilter_view_model_key"
        const val SUBFILTER_TITLE_KEY = "subfilter_title_key"
        const val SUBFILTER_CATEGORIES_KEY = "subfilter_categories_key"

        @JvmStatic
        fun newInstance(
            subfilterViewModelKey: String,
            categories: List<SubfilterCategory>,
            title: CharSequence
        ): SubfilterBottomSheetFragment {
            val fragment = SubfilterBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString(SUBFILTER_VIEW_MODEL_KEY, subfilterViewModelKey)
            bundle.putCharSequence(SUBFILTER_TITLE_KEY, title)
            bundle.putParcelableArrayList(SUBFILTER_CATEGORIES_KEY, ArrayList(categories))

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

        val subfilterVmKey = requireArguments().getString(SUBFILTER_VIEW_MODEL_KEY)!!
        val bottomSheetTitle = requireArguments().getCharSequence(SUBFILTER_TITLE_KEY)!!
        val categories = requireArguments().getParcelableArrayListCompat<SubfilterCategory>(SUBFILTER_CATEGORIES_KEY)!!

        viewModel = ViewModelProvider(
            parentFragment as ViewModelStoreOwner,
            viewModelFactory
        )[subfilterVmKey, SubFilterViewModel::class.java]

        val pager = view.findViewById<ViewPager>(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val title = view.findViewById<TextView>(R.id.title)
        title.text = bottomSheetTitle
        pager.adapter = SubfilterPagerAdapter(
            requireActivity(),
            childFragmentManager,
            subfilterVmKey,
            categories.toList()
        )
        tabLayout.setupWithViewPager(pager)
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

        viewModel.filtersMatchCount.observe(this, Observer {
            for (category in it.keys) {
                val tab = tabLayout.getTabAt(category.ordinal)
                tab?.let { sectionTab ->
                    sectionTab.text = "${view.context.getString(category.titleRes)} (${it[category]})"
                }
            }
        })

        dialog?.setOnShowListener { dialogInterface ->
            val sheetDialog = dialogInterface as? BottomSheetDialog

            val bottomSheet = sheetDialog?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as? FrameLayout

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val metrics = resources.displayMetrics
                behavior.peekHeight = metrics.heightPixels / 2
            }
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
