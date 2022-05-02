package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderFragmentLayoutBinding
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
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState.TabUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.observeEvent
import java.util.EnumSet
import javax.inject.Inject

class ReaderFragment : Fragment(R.layout.reader_fragment_layout), ScrollableViewInitializedListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: ReaderViewModel

    private var searchMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null

    private var binding: ReaderFragmentLayoutBinding? = null

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
        binding = ReaderFragmentLayoutBinding.bind(view).apply {
            initToolbar()
            initViewPager()
            initViewModel()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchMenuItem = null
        settingsMenuItem = null
        binding = null
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

    private fun ReaderFragmentLayoutBinding.initToolbar() {
        toolbar.title = getString(string.reader_screen_title)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
    }

    private fun ReaderFragmentLayoutBinding.initViewPager() {
        viewPager.registerOnPageChangeCallback(viewPagerCallback)
    }

    private fun ReaderFragmentLayoutBinding.initViewModel() {
        viewModel = ViewModelProvider(this@ReaderFragment, viewModelFactory).get(ReaderViewModel::class.java)
        startObserving()
    }

    private fun ReaderFragmentLayoutBinding.startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            uiState?.let {
                when (it) {
                    is ContentUiState -> {
                        updateTabs(it)
                    }
                }
                uiHelpers.updateVisibility(tabLayout, uiState.tabLayoutVisible)
                searchMenuItem?.isVisible = uiState.searchIconVisible
                settingsMenuItem?.isVisible = uiState.settingsIconVisible
            }
        }

        viewModel.updateTags.observeEvent(viewLifecycleOwner) {
            ReaderUpdateServiceStarter.startService(context, EnumSet.of(TAGS, FOLLOWED_BLOGS))
        }

        viewModel.selectTab.observeEvent(viewLifecycleOwner) { navTarget ->
            viewPager.setCurrentItem(navTarget.position, navTarget.smoothAnimation)
        }

        viewModel.showSearch.observeEvent(viewLifecycleOwner) {
            ReaderActivityLauncher.showReaderSearch(context)
        }

        viewModel.showSettings.observeEvent(viewLifecycleOwner) {
            ReaderActivityLauncher.showReaderSubs(context)
        }

        viewModel.showReaderInterests.observeEvent(viewLifecycleOwner) {
            showReaderInterests()
        }

        viewModel.closeReaderInterests.observeEvent(viewLifecycleOwner) {
            closeReaderInterests()
        }

        viewModel.start()
    }

    private fun ReaderFragmentLayoutBinding.updateTabs(uiState: ContentUiState) {
        updateViewPagerAdapterAndMediator(uiState)
        uiState.tabUiStates.forEachIndexed { index, tabUiState ->
            val tab = tabLayout.getTabAt(index) as TabLayout.Tab
            updateTab(tab, tabUiState)
        }
    }

    private fun ReaderFragmentLayoutBinding.updateTab(tab: TabLayout.Tab, tabUiState: TabUiState) {
        val customView = tab.customView ?: createTabCustomView(tab)
        with(customView) {
            val title = findViewById<TextView>(R.id.tab_label)
            title.text = uiHelpers.getTextOfUiString(requireContext(), tabUiState.label)
        }
    }

    private fun ReaderFragmentLayoutBinding.updateViewPagerAdapterAndMediator(uiState: ContentUiState) {
        viewPager.adapter = TabsAdapter(this@ReaderFragment, uiState.readerTagList)
        TabLayoutMediator(tabLayout, viewPager, ReaderTabConfigurationStrategy(uiState)).attach()
    }

    private inner class ReaderTabConfigurationStrategy(
        private val uiState: ContentUiState
    ) : TabLayoutMediator.TabConfigurationStrategy {
        override fun onConfigureTab(@NonNull tab: TabLayout.Tab, position: Int) {
            binding?.updateTab(tab, uiState.tabUiStates[position])
        }
    }

    private fun ReaderFragmentLayoutBinding.createTabCustomView(tab: TabLayout.Tab): View {
        val customView = LayoutInflater.from(context)
                .inflate(R.layout.tab_custom_view, tabLayout, false)
        tab.customView = customView
        return customView
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
        binding?.appBar?.liftOnScrollTargetViewId = containerId
    }
}
