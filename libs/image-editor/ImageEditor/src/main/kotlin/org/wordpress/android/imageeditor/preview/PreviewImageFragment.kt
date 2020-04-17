package org.wordpress.android.imageeditor.preview

import android.app.Activity
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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.CENTER_CROP
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.yalantis.ucrop.UCrop
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.preview_image_fragment.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.RequestListener
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.R.string
import org.wordpress.android.imageeditor.crop.CropFragment
import org.wordpress.android.imageeditor.crop.CropViewModel.CropResult
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageStartLoadingToFileState
import org.wordpress.android.imageeditor.utils.UiHelpers
import java.io.File

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private var pagerAdapterObserver: PagerAdapterObserver? = null
    private lateinit var pageChangeCallback: OnPageChangeCallback

    private var cropActionMenu: MenuItem? = null
    // TODO We might want to move this into the VM
    private var numberOfImages = 0

    companion object {
        const val ARG_EDIT_IMAGE_DATA = "arg_edit_image_data"
        const val PREVIEW_IMAGE_REDUCED_SIZE_FACTOR = 0.1

        sealed class EditImageData : Parcelable {
            @Parcelize
            data class InputData(
                val highResImgUrl: String,
                val lowResImgUrl: String,
                val outputFileExtension: String
            ) : EditImageData()

            @Parcelize
            data class OutputData(val outputFilePath: String) : EditImageData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.preview_image_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullIntent = checkNotNull(requireActivity().intent)
        initializeViewModels(nonNullIntent)
        initializeViews()
    }

    private fun initializeViews() {
        initializeViewPager()
        initializeInsertButton()
    }

    private fun initializeViewPager() {
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
            val imageData = previewImageAdapter.currentList[position].data
            loadIntoImageView(imageData.lowResImageUrl, imageView)
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
        viewModel.uiState.value?.let {
            (previewImageViewPager.adapter as PreviewImageAdapter).submitList(it.viewPagerItemsStates)
            cropActionMenu?.isEnabled = it.editActionsEnabled
            UiHelpers.updateVisibility(thumbnailsTabLayout, it.thumbnailsTabLayoutVisible)
        }
    }

    private fun initializeInsertButton() {
        insertButton.text = getString(string.insert_label_with_count, numberOfImages)
        insertButton.setOnClickListener {
            Log.d("PreviewImageFragment", "Insert button clicked")
        }
    }

    private fun initializeViewModels(nonNullIntent: Intent) {
        viewModel = ViewModelProvider(this).get(PreviewImageViewModel::class.java)
        setupObservers()
        val inputData = nonNullIntent.getParcelableArrayListExtra<EditImageData.InputData>(ARG_EDIT_IMAGE_DATA)
        numberOfImages = inputData.size

        viewModel.onCreateView(inputData)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, Observer { state ->
            (previewImageViewPager.adapter as PreviewImageAdapter).submitList(state.viewPagerItemsStates)
            cropActionMenu?.isEnabled = state.editActionsEnabled
            UiHelpers.updateVisibility(thumbnailsTabLayout, state.thumbnailsTabLayoutVisible)
        })

        viewModel.loadIntoFile.observe(viewLifecycleOwner, Observer { fileStateEvent ->
            fileStateEvent?.getContentIfNotHandled()?.let { fileState ->
                if (fileState is ImageStartLoadingToFileState) {
                    loadIntoFile(fileState.imageUrlAtPosition, fileState.position)
                }
            }
        })

        viewModel.navigateToCropScreenWithFileInfo.observe(viewLifecycleOwner, Observer { fileInfoEvent ->
            fileInfoEvent?.getContentIfNotHandled()?.let { fileInfo ->
                navigateToCropScreenWithFileInfo(fileInfo)
            }
        })

        findNavController().currentBackStackEntry?.savedStateHandle
                ?.getLiveData<CropResult>(CropFragment.CROP_RESULT)?.observe(
                viewLifecycleOwner, Observer { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data
                if (data.hasExtra(UCrop.EXTRA_OUTPUT_URI)) {
                    val imageUri = data.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI) as? Uri
                    imageUri?.let {
                        viewModel.onCropResult(it.toString())
                    }
                }
            }
        })
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
            url,
            object : RequestListener<File> {
                override fun onResourceReady(resource: File, url: String) {
                    viewModel.onLoadIntoFileSuccess(resource.path, position)
                }

                override fun onLoadFailed(e: Exception?, url: String) {
                    viewModel.onLoadIntoFileFailed()
                }
            }
        )
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
        viewModel.onCropMenuClicked(previewImageViewPager.currentItem)
        true
    } else {
        super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagerAdapterObserver?.let {
            previewImageViewPager.adapter?.unregisterAdapterDataObserver(it)
        }
        pagerAdapterObserver = null
        tabLayoutMediator.detach()
        previewImageViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }
}
