package org.wordpress.android.ui.reader

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.reader_fragment_layout.*
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderDiscoverFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.FOLLOWED_BLOGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.viewmodels.NewsCardViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.InitialUiState
import org.wordpress.android.ui.utils.UiHelpers
import java.util.EnumSet
import javax.inject.Inject

class ReaderFragment : Fragment(R.layout.reader_fragment_layout) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: ReaderViewModel
    private lateinit var newsCardViewModel: NewsCardViewModel

    private var searchMenuItem: MenuItem? = null

    private val viewPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.uiState.value?.let {
                val currentUiState = it as ContentUiState
                val selectedTag = currentUiState.readerTagList[position]
                newsCardViewModel.onTagChanged(selectedTag)
                viewModel.onTagChanged(selectedTag)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        initToolbar()
        initViewPager()
        initViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchMenuItem = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.onScreenInForeground()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onScreenInBackground()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.reader_home, menu)
        menu.findItem(R.id.menu_search).apply {
            searchMenuItem = this
            this.isVisible = viewModel.uiState.value?.searchIconVisible ?: false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_search) {
            viewModel.onSearchActionClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun initToolbar() {
        toolbar.title = getString(string.reader_screen_title)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    }

    private fun initViewPager() {
        view_pager.registerOnPageChangeCallback(viewPagerCallback)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderViewModel::class.java)
        newsCardViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(NewsCardViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            uiState?.let {
                when (it) {
                    is InitialUiState -> {
                    }
                    is ContentUiState -> {
                        updateTabs(it)
                    }
                }
                app_bar.setExpanded(uiState.appBarExpanded)
                uiHelpers.updateVisibility(tab_layout, uiState.tabLayoutVisible)
                searchMenuItem?.isVisible = uiState.searchIconVisible
            }
        })

        viewModel.updateTags.observe(viewLifecycleOwner, Observer { updateAcion ->
            updateAcion?.getContentIfNotHandled()?.let {
                ReaderUpdateServiceStarter.startService(context, EnumSet.of(TAGS, FOLLOWED_BLOGS))
            }
        })

        viewModel.selectTab.observe(viewLifecycleOwner, Observer { selectTabAction ->
            selectTabAction.getContentIfNotHandled()?.let { tabPosition ->
                view_pager.setCurrentItem(tabPosition, false)
            }
        })

        viewModel.showSearch.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let {
                ReaderActivityLauncher.showReaderSearch(context)
            }
        })

        viewModel.showReaderInterests.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                showReaderInterests()
            }
        })

        viewModel.closeReaderInterests.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                closeReaderInterests()
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

    private fun updateTabs(uiState: ContentUiState) {
        val adapter = TabsAdapter(this, uiState.readerTagList)
        view_pager.adapter = adapter

        TabLayoutMediator(tab_layout, view_pager) { tab, position ->
            tab.text = uiState.tabTitles[position]
        }.attach()
    }

    private class TabsAdapter(parent: Fragment, private val tags: ReaderTagList) : FragmentStateAdapter(parent) {
        override fun getItemCount(): Int = tags.size

        override fun createFragment(position: Int): Fragment {
            return if (AppPrefs.isReaderImprovementsPhase2Enabled() && tags[position].isDiscover) {
                ReaderDiscoverFragment()
            } else {
                ReaderPostListFragment.newInstanceForTag(tags[position], ReaderPostListType.TAG_FOLLOWED, true)
            }
        }
    }

    private fun showReaderInterests() {
        val readerInterestsFragment = childFragmentManager.findFragmentByTag(ReaderInterestsFragment.TAG)
        if (readerInterestsFragment == null) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.interests_fragment_container,
                    ReaderInterestsFragment(),
                    ReaderInterestsFragment.TAG
                )
                .commitNow()
        }
    }

    private fun closeReaderInterests() {
        val readerInterestsFragment = childFragmentManager.findFragmentByTag(ReaderInterestsFragment.TAG)
        if (readerInterestsFragment?.isAdded == true) {
            childFragmentManager.beginTransaction()
                .remove(readerInterestsFragment)
                .commitNow()
        }
    }
}
