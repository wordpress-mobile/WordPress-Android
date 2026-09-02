package org.wordpress.android.ui.posts.prepublishing.featuredimage

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView.ScaleType
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PrepublishingFeaturedImageFragmentBinding
import org.wordpress.android.databinding.PrepublishingToolbarBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditorDataProvider
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageState
import org.wordpress.android.ui.posts.prepublishing.PrepublishingViewModel
import org.wordpress.android.ui.posts.prepublishing.featuredimage.PrepublishingFeaturedImageViewModel.UiState
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingScreenClosedListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingFeaturedImageFragment : Fragment(R.layout.prepublishing_featured_image_fragment) {
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var mediaPickerLauncher: MediaPickerLauncher

    @Inject
    lateinit var dispatcher: Dispatcher

    private lateinit var viewModel: PrepublishingFeaturedImageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun onResume() {
        super.onResume()
        // The media picker returns its result to the host activity, so refresh here to reflect
        // any image that was just set or queued for upload while this screen was in the background.
        if (::viewModel.isInitialized) {
            viewModel.refreshFeaturedImageState()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PrepublishingFeaturedImageFragmentBinding.bind(view)) {
            includePrepublishingToolbar.initToolbar()
            initClickListeners()
            initViewModel()
        }
    }

    private fun PrepublishingToolbarBinding.initToolbar() {
        toolbarTitle.text = getString(R.string.prepublishing_nudges_toolbar_title_featured_image)
        backButton.setOnClickListener { viewModel.onBackButtonClick() }
    }

    private fun PrepublishingFeaturedImageFragmentBinding.initClickListeners() {
        buttonSetFeaturedImage.setOnClickListener { viewModel.onSetOrChangeClicked() }
        buttonRemoveFeaturedImage.setOnClickListener { viewModel.onRemoveClicked() }
        featuredImageRetryOverlay.setOnClickListener { viewModel.onRetryClicked() }
    }

    private fun PrepublishingFeaturedImageFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(
            this@PrepublishingFeaturedImageFragment,
            viewModelFactory
        )[PrepublishingFeaturedImageViewModel::class.java]

        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            renderUiState(uiState)
        }

        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner) {
            closeListener?.onBackClicked()
        }

        viewModel.launchFeaturedImagePicker.observeEvent(viewLifecycleOwner) { postId ->
            mediaPickerLauncher.showFeaturedImagePicker(requireActivity(), getSite(), postId)
        }

        // updateAsync mutates the shared post and fires postChanged, so this catches an image set
        // from the media library, an upload that just completed, and a removal.
        getEditPostRepository().postChanged.observeEvent(viewLifecycleOwner) {
            viewModel.refreshFeaturedImageState()
        }

        viewModel.start(getEditPostRepository(), getSite())
    }

    private fun PrepublishingFeaturedImageFragmentBinding.renderUiState(uiState: UiState) {
        val state = uiState.featuredImageData.uiState
        val hasImage = state != FeaturedImageState.IMAGE_EMPTY
        with(uiHelpers) {
            updateVisibility(featuredImageEmptyText, !hasImage)
            updateVisibility(postFeaturedImageContainer, hasImage)
            updateVisibility(postFeaturedImage, state.imageViewVisible)
            updateVisibility(postFeaturedImageLocal, state.localImageViewVisible)
            updateVisibility(featuredImageRetryOverlay, state.retryOverlayVisible)
            updateVisibility(featuredImageProgressOverlay, state.progressOverlayVisible)
            updateVisibility(buttonRemoveFeaturedImage, uiState.isRemoveButtonVisible)
        }
        buttonSetFeaturedImage.text = uiHelpers.getTextOfUiString(requireContext(), uiState.setButtonText)
        if (!state.localImageViewVisible) {
            imageManager.cancelRequestAndClearImageView(postFeaturedImageLocal)
        }
        loadFeaturedImage(uiState)
    }

    private fun PrepublishingFeaturedImageFragmentBinding.loadFeaturedImage(uiState: UiState) {
        val mediaUri = uiState.featuredImageData.mediaUri ?: return
        if (uiState.featuredImageData.uiState == FeaturedImageState.REMOTE_IMAGE_LOADING) {
            // Keep the local image (if any) visible until the remote image is ready to avoid a flash
            // of an empty view when the local image is swapped for the remote one.
            imageManager.loadWithResultListener(
                postFeaturedImage, ImageType.IMAGE, mediaUri, ScaleType.FIT_CENTER, null,
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {}
                    override fun onResourceReady(resource: Drawable, model: Any?) {
                        with(uiHelpers) {
                            updateVisibility(postFeaturedImage, true)
                            updateVisibility(postFeaturedImageLocal, false)
                        }
                    }
                }
            )
        } else {
            imageManager.load(postFeaturedImageLocal, ImageType.IMAGE, mediaUri, ScaleType.FIT_CENTER)
        }
    }

    private fun getSite(): SiteModel =
        requireNotNull(arguments?.getSerializableCompat<SiteModel>(WordPress.SITE))

    private fun getEditPostRepository(): EditPostRepository {
        val activity = activity
        val editorDataProvider = if (activity is EditorDataProvider) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditorDataProvider")
        }
        return editorDataProvider.editPostRepository
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        val media = event.media
        if (media != null && media.markedLocallyAsFeatured) {
            viewModel.refreshFeaturedImageState()
        }
    }

    companion object {
        const val TAG = "prepublishing_featured_image_fragment_tag"

        @JvmStatic
        fun newInstance(site: SiteModel): PrepublishingFeaturedImageFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
            }
            return PrepublishingFeaturedImageFragment().apply { arguments = bundle }
        }
    }
}
