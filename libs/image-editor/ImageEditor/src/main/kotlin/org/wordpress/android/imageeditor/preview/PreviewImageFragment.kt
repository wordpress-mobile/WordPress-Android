package org.wordpress.android.imageeditor.preview

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.CENTER_CROP
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.yalantis.ucrop.UCrop
import kotlinx.parcelize.Parcelize
import org.wordpress.android.imageeditor.EditImageViewModel
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.RequestListener
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.databinding.PreviewImageFragmentBinding
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageStartLoadingToFileState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.UiState
import org.wordpress.android.imageeditor.utils.ToastUtils
import org.wordpress.android.imageeditor.utils.ToastUtils.Duration
import org.wordpress.android.imageeditor.utils.UiHelpers
import java.io.File

class PreviewImageFragment : Fragment(R.layout.preview_image_fragment) {
    private var binding: PreviewImageFragmentBinding? = null
    private lateinit var viewModel: PreviewImageViewModel
    private lateinit var parentViewModel: EditImageViewModel
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private var pagerAdapterObserver: PagerAdapterObserver? = null
    private lateinit var pageChangeCallback: OnPageChangeCallback

    private var cropActionMenu: MenuItem? = null

    companion object {
        private val TAG = PreviewImageFragment::class.java.simpleName
        const val ARG_EDIT_IMAGE_DATA = "arg_edit_image_data"
        const val PREVIEW_IMAGE_REDUCED_SIZE_FACTOR = 0.1

        @SuppressLint("ParcelCreator")
        sealed class EditImageData : Parcelable {
            @Parcelize
            data class InputData(
                val highResImgUrl: String,
                val lowResImgUrl: String?,
                val outputFileExtension: String?
            ) : EditImageData()

            @Parcelize
            data class OutputData(val outputFilePath: String) : EditImageData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PreviewImageFragmentBinding.bind(view)) {
            binding = this
            val nonNullIntent = checkNotNull(requireActivity().intent)
            initializeViewModels(nonNullIntent)
            initializeViews()
        }
    }

    private fun PreviewImageFragmentBinding.initializeViews() {
        initializeViewPager()
        initializeInsertButton()
    }

    private fun PreviewImageFragmentBinding.initializeViewPager() {
        val previewImageAdapter = PreviewImageAdapter(
                loadIntoImageViewWithResultListener = { imageData, imageView, position ->
                    loadIntoImageViewWithResultListener(imageData, imageView, position)
                }
        )
        previewImageAdapter.setHasStableIds(true)
        previewImageViewPager.adapter = previewImageAdapter

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.onPageSelected(position)
            }
        }
        previewImageViewPager.registerOnPageChangeCallback(pageChangeCallback)

        val tabConfigurationStrategy = TabLayoutMediator.TabConfigurationStrategy { tab, position ->
            if (tab.customView == null) {
                val customView = LayoutInflater.from(context)
                        .inflate(R.layout.preview_image_thumbnail, thumbnailsTabLayout, false)
                tab.customView = customView
            }
            val imageView = (tab.customView as FrameLayout).findViewById<ImageView>(R.id.thumbnailImageView)
            loadIntoImageView(viewModel.getThumbnailImageUrl(position), imageView)
        }

        tabLayoutMediator = TabLayoutMediator(
                thumbnailsTabLayout,
                previewImageViewPager,
                false,
                tabConfigurationStrategy
        )
        tabLayoutMediator.attach()

        pagerAdapterObserver = PagerAdapterObserver(
                thumbnailsTabLayout,
                previewImageViewPager,
                tabConfigurationStrategy
        )
        previewImageAdapter.registerAdapterDataObserver(pagerAdapterObserver as PagerAdapterObserver)

        // Setting page transformer explicitly sets internal RecyclerView's itemAnimator to null
        // to fix this issue: https://issuetracker.google.com/issues/37034191
        previewImageViewPager.setPageTransformer { _, _ ->
        }

        // Set adapter data before the ViewPager2.restorePendingState gets called
        // to avoid manual handling of the ViewPager2 state restoration.
        viewModel.uiState.value?.let { updateUiState(it) }
    }

    private fun PreviewImageFragmentBinding.initializeInsertButton() {
        insertButton.text = getString(R.string.insert_label_with_count, viewModel.numberOfImages)
        insertButton.setOnClickListener {
            viewModel.onInsertClicked()
        }
    }

    private fun PreviewImageFragmentBinding.initializeViewModels(nonNullIntent: Intent) {
        viewModel = ViewModelProvider(this@PreviewImageFragment).get(PreviewImageViewModel::class.java)
        parentViewModel = ViewModelProvider(requireActivity()).get(EditImageViewModel::class.java)
        setupObservers()
        val inputData = nonNullIntent.getParcelableArrayListExtra<EditImageData.InputData>(ARG_EDIT_IMAGE_DATA)

        inputData?.let { viewModel.onCreateView(it, ImageEditor.instance) }
    }

    private fun PreviewImageFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, { state -> updateUiState(state) })

        viewModel.loadIntoFile.observe(viewLifecycleOwner, { fileStateEvent ->
            fileStateEvent?.getContentIfNotHandled()?.let { fileState ->
                when (fileState) {
                    is ImageLoadToFileIdleState -> { // Do nothing
                    }
                    is ImageStartLoadingToFileState -> {
                        loadIntoFile(fileState.imageUrlAtPosition, fileState.position)
                    }
                    is ImageLoadToFileFailedState -> {
                        showFileLoadError(fileState.errorMsg, fileState.errorResId)
                    }
                    is ImageLoadToFileSuccessState -> { // Do nothing
                    }
                }
            }
        })

        viewModel.navigateToCropScreenWithFileInfo.observe(viewLifecycleOwner, { fileInfoEvent ->
            fileInfoEvent?.getContentIfNotHandled()?.let { fileInfo ->
                navigateToCropScreenWithFileInfo(fileInfo)
            }
        })

        parentViewModel.cropResult.observe(viewLifecycleOwner, { cropResult ->
            cropResult?.getContentIfNotHandled()?.let {
                if (it.resultCode == RESULT_OK) {
                    val data: Intent = it.data
                    if (data.hasExtra(UCrop.EXTRA_OUTPUT_URI)) {
                        val imageUri = data.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI) as? Uri
                        imageUri?.let { uri ->
                            viewModel.onCropResult(uri.toString())
                        }
                    }
                }
            }
        })

        viewModel.finishAction.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                val intent = Intent().apply { putParcelableArrayListExtra(ARG_EDIT_IMAGE_DATA, ArrayList(it)) }
                requireActivity().setResult(RESULT_OK, intent)
                requireActivity().finish()
            }
        })
    }

    private fun PreviewImageFragmentBinding.updateUiState(state: UiState) {
        (previewImageViewPager.adapter as PreviewImageAdapter).submitList(state.viewPagerItemsStates)
        cropActionMenu?.isEnabled = state.editActionsEnabled
        UiHelpers.updateVisibility(thumbnailsTabLayout, state.thumbnailsTabLayoutVisible)
        UiHelpers.updateVisibility(insertButton, state.thumbnailsTabLayoutVisible)
    }

    private fun loadIntoImageView(url: String, imageView: ImageView) {
        ImageEditor.instance.loadIntoImageView(url, imageView, CENTER_CROP)
    }

    private fun loadIntoImageViewWithResultListener(imageData: ImageData, imageView: ImageView, position: Int) {
        ImageEditor.instance.loadIntoImageViewWithResultListener(
                imageData.highResImageUrl,
                imageView,
                CENTER,
                imageData.lowResImageUrl,
                object : RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable, url: String) {
                        viewModel.onLoadIntoImageViewSuccess(url, position)
                    }

                    override fun onLoadFailed(e: Exception?, url: String) {
                        viewModel.onLoadIntoImageViewFailed(url, position)
                    }
                }
        )
    }

    private fun loadIntoFile(url: String, position: Int) {
        ImageEditor.instance.loadIntoFileWithResultListener(
                Uri.parse(url),
                object : RequestListener<File> {
                    override fun onResourceReady(resource: File, url: String) {
                        viewModel.onLoadIntoFileSuccess(resource.path, position)
                    }

                    override fun onLoadFailed(e: Exception?, url: String) {
                        viewModel.onLoadIntoFileFailed(e)
                    }
                }
        )
    }

    private fun showFileLoadError(errorMsg: String?, errorResId: Int) {
        Log.e(TAG, "Failed to load into file: $errorMsg")
        ToastUtils.showToast(context, getString(errorResId), Duration.LONG)
    }

    private fun navigateToCropScreenWithFileInfo(fileInfo: Triple<String, String?, Boolean>) {
        val (inputFilePath, outputFileExtension, shouldReturnToPreviewScreen) = fileInfo

        val navOptions = if (!shouldReturnToPreviewScreen) {
            NavOptions.Builder().setPopUpTo(R.id.preview_dest, true).build()
        } else {
            null
        }

        val navController = findNavController()

        // TODO: Temporarily added if check to fix this occasional crash
        // https://stackoverflow.com/q/51060762/193545
        if (navController.currentDestination?.id == R.id.preview_dest) {
            navController.navigate(
                    PreviewImageFragmentDirections.actionPreviewFragmentToCropFragment(
                            inputFilePath,
                            outputFileExtension,
                            shouldReturnToPreviewScreen
                    ),
                    navOptions
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_preview_fragment, menu)
        cropActionMenu = menu.findItem(R.id.menu_crop)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == R.id.menu_crop) {
        viewModel.onCropMenuClicked()
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagerAdapterObserver?.let {
            binding?.previewImageViewPager?.adapter?.unregisterAdapterDataObserver(it)
        }
        pagerAdapterObserver = null
        tabLayoutMediator.detach()
        binding?.previewImageViewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        binding = null
    }
}
