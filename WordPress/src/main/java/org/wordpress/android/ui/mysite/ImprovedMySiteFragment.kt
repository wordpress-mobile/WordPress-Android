package org.wordpress.android.ui.mysite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import kotlinx.android.synthetic.main.me_action_layout.*
import kotlinx.android.synthetic.main.new_my_site_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteViewModel.State
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.USER
import org.wordpress.android.util.setVisible
import javax.inject.Inject

class ImprovedMySiteFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var meGravatarLoader: MeGravatarLoader
    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper
    @Inject lateinit var mySiteNavigationActionHandler: MySiteNavigationActionHandler
    private lateinit var viewModel: MySiteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MySiteViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
                R.layout.new_my_site_fragment,
                container,
                false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appbar_main.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxOffset = appBarLayout.totalScrollRange
            val currentOffset = maxOffset + verticalOffset

            val percentage = ((currentOffset.toFloat() / maxOffset.toFloat()) * 100).toInt()
            avatar?.let {
                val minSize = avatar.minimumHeight
                val maxSize = avatar.maxHeight
                val modifierPx = (minSize.toFloat() - maxSize.toFloat()) * (percentage.toFloat() / 100) * -1
                val modifierPercentage = modifierPx / minSize
                val newScale = 1 + modifierPercentage

                avatar.scaleX = newScale
                avatar.scaleY = newScale
            }
        })

        toolbar_main.let { toolbar ->
            toolbar.inflateMenu(R.menu.my_site_menu)
            toolbar.menu.findItem(R.id.me_item)?.let { meMenu ->
                meMenu.actionView.let { actionView ->
                    actionView.setOnClickListener { viewModel.onAvatarPressed() }
                    TooltipCompat.setTooltipText(actionView, meMenu.title)
                }
            }
        }

        actionable_empty_view.button.setOnClickListener { viewModel.onAddSitePressed() }

        viewModel.uiModel.observe(viewLifecycleOwner, {
            it?.let { uiModel ->
                loadGravatar(uiModel.accountAvatarUrl)
                when (val state = uiModel.state) {
                    is State.SiteSelected -> loadData()
                    is State.NoSites -> loadEmptyView(state.shouldShowImage)
                }
            }
        })
        viewModel.onSiteSelected.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { siteId ->
                showFragment(MySiteContentFragment.newInstance(siteId))
            }
        })
        viewModel.onNavigation.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { action ->
                mySiteNavigationActionHandler.navigate(requireActivity(), this, action)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun loadGravatar(avatarUrl: String) = avatar?.let {
        meGravatarLoader.load(
                false,
                meGravatarLoader.constructGravatarUrl(avatarUrl),
                null,
                it,
                USER,
                null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            return
        }
        childFragmentManager.findFragmentByTag(CONTENT_FRAGMENT_TAG)?.onActivityResult(requestCode, resultCode, data)
    }

    private fun loadData() {
        content_container.setVisible(true)
        actionable_empty_view.setVisible(false)
    }

    private fun showFragment(fragment: Fragment) {
        val fragmentTransaction: FragmentTransaction = childFragmentManager.beginTransaction()
        childFragmentManager.findFragmentByTag(CONTENT_FRAGMENT_TAG)?.let { previousFragment ->
            fragmentTransaction.remove(previousFragment)
        }
        fragmentTransaction.replace(R.id.content_container, fragment, CONTENT_FRAGMENT_TAG)
        fragmentTransaction.commit()
    }

    private fun loadEmptyView(shouldShowEmptyViewImage: Boolean) {
        content_container.setVisible(false)
        actionable_empty_view.setVisible(true)
        actionable_empty_view.image.setVisible(shouldShowEmptyViewImage)
    }

    companion object {
        private const val CONTENT_FRAGMENT_TAG = "content_fragment"
        fun newInstance(): ImprovedMySiteFragment {
            return ImprovedMySiteFragment()
        }
    }
}
