package org.wordpress.android.ui.posts.prepublishing.featuredimage

import android.content.Context
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
import org.wordpress.android.ui.posts.prepublishing.featuredimage.PrepublishingFeaturedImageViewModel.UiState
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingScreenClosedListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.image.ImageManager
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

        viewModel.syncFeaturedImageToEditor.observeEvent(viewLifecycleOwner) {
            getEditorDataProvider().syncFeaturedImageIdToEditor()
        }

        // updateAsync mutates the shared post and fires postChanged, so this catches an image set
        // from the media library, an upload that just completed, and a removal. Observe it raw
        // (not observeEvent): postChanged is a single-consumption Event with other observers, so
        // observeEvent would miss it whenever another observer handles it first.
        getEditPostRepository().postChanged.observe(viewLifecycleOwner) {
            viewModel.refreshFeaturedImageState()
        }

        viewModel.start(getEditPostRepository(), getSite())
    }

    // TODO CMM-2376 follow-up: this featured-image rendering (state->visibility, overlays, remote vs
    // local image) mirrors EditPostSettingsFragment.updateFeaturedImageView. Extract a shared
    // component so the two don't drift.
    private fun PrepublishingFeaturedImageFragmentBinding.renderUiState(uiState: UiState) {
        val state = uiState.featuredImageData.uiState
        // A remote image (already on the server) uses postFeaturedImage; a local image being
        // uploaded uses postFeaturedImageLocal. Only ever show one so an empty view can't cover the
        // other in the FrameLayout.
        val isRemote = state == FeaturedImageState.REMOTE_IMAGE_LOADING ||
                state == FeaturedImageState.REMOTE_IMAGE_SET
        val isLocalUpload = state == FeaturedImageState.IMAGE_UPLOAD_IN_PROGRESS ||
                state == FeaturedImageState.IMAGE_UPLOAD_FAILED
        val hasImage = state != FeaturedImageState.IMAGE_EMPTY
        with(uiHelpers) {
            updateVisibility(featuredImageEmptyText, !hasImage)
            updateVisibility(postFeaturedImageContainer, hasImage)
            updateVisibility(postFeaturedImage, isRemote)
            updateVisibility(postFeaturedImageLocal, isLocalUpload)
            updateVisibility(featuredImageRetryOverlay, state.retryOverlayVisible)
            updateVisibility(featuredImageProgressOverlay, state.progressOverlayVisible)
            updateVisibility(buttonRemoveFeaturedImage, uiState.isRemoveButtonVisible)
        }
        buttonSetFeaturedImage.text = uiHelpers.getTextOfUiString(requireContext(), uiState.setButtonText)

        val mediaUri = uiState.featuredImageData.mediaUri
        when {
            isRemote && !mediaUri.isNullOrEmpty() -> {
                imageManager.cancelRequestAndClearImageView(postFeaturedImageLocal)
                imageManager.load(postFeaturedImage, ImageType.IMAGE, mediaUri, ScaleType.FIT_CENTER)
            }
            isLocalUpload && !mediaUri.isNullOrEmpty() ->
                imageManager.load(postFeaturedImageLocal, ImageType.IMAGE, mediaUri, ScaleType.FIT_CENTER)
            else -> {
                imageManager.cancelRequestAndClearImageView(postFeaturedImage)
                imageManager.cancelRequestAndClearImageView(postFeaturedImageLocal)
            }
        }
    }

    private fun getSite(): SiteModel =
        requireNotNull(arguments?.getSerializableCompat<SiteModel>(WordPress.SITE))

    private fun getEditPostRepository(): EditPostRepository = getEditorDataProvider().editPostRepository

    private fun getEditorDataProvider(): EditorDataProvider {
        val activity = activity
        return if (activity is EditorDataProvider) {
            activity
        } else {
            error("$activity must implement EditorDataProvider")
        }
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
