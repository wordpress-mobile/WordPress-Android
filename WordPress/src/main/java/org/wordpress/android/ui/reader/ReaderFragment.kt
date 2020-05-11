package org.wordpress.android.ui.reader

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.FOLLOWED_BLOGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.viewmodels.NewsCardViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState
import java.util.EnumSet
import javax.inject.Inject

class ReaderFragment : Fragment(R.layout.reader_fragment_layout) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderViewModel
    private lateinit var newsCardViewModel: NewsCardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO should we show a loading view before we load the tags?
        initViewModel(view)
    }

    private fun initViewModel(view: View) {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderViewModel::class.java)
        newsCardViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(NewsCardViewModel::class.java)
        startObserving(view)
    }

    private fun startObserving(view: View) {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            uiState?.let {
                initViewPager(uiState, view)
            }
        })

        viewModel.updateTags.observe(viewLifecycleOwner, Observer { updateAcion ->
            updateAcion?.getContentIfNotHandled()?.let {
                ReaderUpdateServiceStarter.startService(context, EnumSet.of(TAGS, FOLLOWED_BLOGS))
            }
        })

        newsCardViewModel.openUrlEvent.observe(viewLifecycleOwner, Observer {
            it?.getContentIfNotHandled()?.let { url ->
                val activity: Activity? = activity
                if (activity != null) {
                    WPWebViewActivity.openURL(activity, url)
                }
            }
        })
        viewModel.start()
    }

    private fun initViewPager(uiState: ReaderUiState, view: View) {
        val adapter = TabsAdapter(this, uiState.readerTagList)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = uiState.tabTitles[position]
        }.attach()

        val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                newsCardViewModel.onTagChanged(uiState.readerTagList[position])
            }
        }
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    private class TabsAdapter(parent: Fragment, private val tags: ReaderTagList) : FragmentStateAdapter(parent) {
        override fun getItemCount(): Int = tags.size

        override fun createFragment(position: Int): Fragment {
            return ReaderPostListFragment.newInstanceForTag(tags[position], ReaderPostListType.TAG_FOLLOWED, true)
        }
    }
}
