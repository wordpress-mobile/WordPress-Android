package org.wordpress.android.ui.reader

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.Organization
import org.wordpress.android.ui.reader.subfilter.SubFilterSharedViewModel
import org.wordpress.android.ui.reader.subfilter.SubfilterPagerAdapter
import javax.inject.Inject

class SubfilterBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SubFilterSharedViewModel

    companion object {
        const val ORGANIZATION_KEY = "organization_key"
        const val CURRENT_PAGE_KEY = "current_page_key"

        @JvmStatic
        fun newInstance(organization: Organization, currentPage: Int): SubfilterBottomSheetFragment {
            val fragment = SubfilterBottomSheetFragment()
            val bundle = Bundle()
            bundle.putSerializable(ORGANIZATION_KEY, organization)
            bundle.putInt(CURRENT_PAGE_KEY, currentPage)
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

        val organization = arguments?.getSerializable(ORGANIZATION_KEY) as Organization
        val currentPage = arguments?.getInt(CURRENT_PAGE_KEY) ?: 0

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(SubFilterSharedViewModel::class.java)

        val pager = view.findViewById<ViewPager>(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        pager.adapter = SubfilterPagerAdapter(requireActivity(), childFragmentManager, organization)
        tabLayout.setupWithViewPager(pager)
        pager.currentItem = currentPage

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
