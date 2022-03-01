package org.wordpress.android.ui.mysite

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MySiteFragmentBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.tabs.MySiteTabsAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MySiteFragment : Fragment(R.layout.my_site_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    private lateinit var viewModel: MySiteViewModel

    private var binding: MySiteFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSoftKeyboard()
        initDagger()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        binding = MySiteFragmentBinding.bind(view).apply {
            setupToolbar()
            setupObservers()
        }
    }

    private fun initSoftKeyboard() {
        // The following prevents the soft keyboard from leaving a white space when dismissed.
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(MySiteViewModel::class.java)
    }

    private fun MySiteFragmentBinding.setupToolbar() {
        toolbarMain.let { toolbar ->
            toolbar.inflateMenu(R.menu.my_site_menu)
            toolbar.menu.findItem(R.id.me_item)?.let { meMenu ->
                meMenu.actionView.let { actionView ->
                    // todo: annmarie - msd - need to add empty view back to my_site_fragment
                    actionView.setOnClickListener { viewModel.onAvatarPressed() }
                    TooltipCompat.setTooltipText(actionView, meMenu.title)
                }
            }
        }
        setupTabs(viewModel.tabTitles)
        val avatar = root.findViewById<ImageView>(R.id.avatar)

        appbarMain.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxOffset = appBarLayout.totalScrollRange
            val currentOffset = maxOffset + verticalOffset

            val percentage = ((currentOffset.toFloat() / maxOffset.toFloat()) * 100).toInt()
            avatar?.let { avatar ->
                val minSize = avatar.minimumHeight
                val maxSize = avatar.maxHeight
                val modifierPx = (minSize.toFloat() - maxSize.toFloat()) * (percentage.toFloat() / 100) * -1
                val modifierPercentage = modifierPx / minSize
                val newScale = 1 + modifierPercentage

                avatar.scaleX = newScale
                avatar.scaleY = newScale
            }
        })
    }

    private fun MySiteFragmentBinding.setupTabs(tabTitles: List<UiString>) {
        val adapter = MySiteTabsAdapter(this@MySiteFragment, tabTitles)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = uiHelpers.getTextOfUiString(requireContext(), tabTitles[position])
        }.attach()

        // todo: annmarie - do something more clever here
        tabLayout.setVisible(true)
    }

    private fun MySiteFragmentBinding.setupObservers() {
        viewModel.onNavigation.observeEvent(viewLifecycleOwner, { handleNavigationAction(it) })
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
        is SiteNavigationAction.OpenMeScreen -> ActivityLauncher.viewMeActivityForResult(activity)
        is SiteNavigationAction.AddNewSite -> SitePickerActivity.addSite(activity, action.hasAccessToken)
        else -> {
            // Ignore any other navigation events
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(): MySiteFragment {
            return MySiteFragment()
        }
    }
}
