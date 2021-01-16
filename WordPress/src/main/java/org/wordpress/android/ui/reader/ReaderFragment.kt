package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.reader_fragment_layout.*
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderDiscoverFragment
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.FOLLOWED_BLOGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.utils.UiHelpers
import java.util.EnumSet
import javax.inject.Inject

class ReaderFragment : Fragment(R.layout.reader_fragment_layout), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: ReaderViewModel

    private var searchMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null

    private val viewPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.uiState.value?.let {
                if (it is ContentUiState) {
                    val selectedTag = it.readerTagList[position]
                    viewModel.onTagChanged(selectedTag)
                }
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
        settingsMenuItem = null
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
        menu.findItem(R.id.menu_settings).apply {
            settingsMenuItem = this
            this.isVisible = viewModel.uiState.value?.settingsIconVisible ?: false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                viewModel.onSearchActionClicked()
                true
            }
            R.id.menu_settings -> {
                viewModel.onSettingsActionClicked()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
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
        viewModel = ViewModelProvider(this, viewModelFactory).get(ReaderViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            uiState?.let {
                when (it) {
                    is ContentUiState -> {
                        updateTabs(it)
                    }
                }
                uiHelpers.updateVisibility(tab_layout, uiState.tabLayoutVisible)
                searchMenuItem?.isVisible = uiState.searchIconVisible
                settingsMenuItem?.isVisible = uiState.settingsIconVisible
            }
        })

        viewModel.updateTags.observe(viewLifecycleOwner, Observer { updateAcion ->
            updateAcion?.getContentIfNotHandled()?.let {
                ReaderUpdateServiceStarter.startService(context, EnumSet.of(TAGS, FOLLOWED_BLOGS))
            }
        })

        viewModel.selectTab.observe(viewLifecycleOwner, Observer { selectTabAction ->
            selectTabAction.getContentIfNotHandled()?.let { navTarget ->
                view_pager.setCurrentItem(navTarget.position, navTarget.smoothAnimation)
            }
        })

        viewModel.showSearch.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let {
                ReaderActivityLauncher.showReaderSearch(context)
            }
        })

        viewModel.showSettings.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let {
                ReaderActivityLauncher.showReaderSubs(context)
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

        viewModel.start()
    }

    private fun updateTabs(uiState: ContentUiState) {
        val adapter = TabsAdapter(this, uiState.readerTagList)
        view_pager.adapter = adapter

        TabLayoutMediator(tab_layout, view_pager) { tab, position ->
            tab.text = uiState.tabTitles[position]
        }.attach()
    }

    fun requestBookmarkTab() {
        viewModel.bookmarkTabRequested()
    }

    private class TabsAdapter(parent: Fragment, private val tags: ReaderTagList) : FragmentStateAdapter(parent) {
        override fun getItemCount(): Int = tags.size

        override fun createFragment(position: Int): Fragment {
            return if (tags[position].isDiscover) {
                ReaderDiscoverFragment()
            } else {
                ReaderPostListFragment.newInstanceForTag(
                        tags[position],
                        ReaderPostListType.TAG_FOLLOWED,
                        true
                )
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

    override fun onScrollableViewInitialized(containerId: Int) {
        app_bar.liftOnScrollTargetViewId = containerId
    }
}
