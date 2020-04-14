package org.wordpress.android.imageeditor.preview

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.preview_image_fragment.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.RequestListener
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import java.io.File

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel
    private lateinit var tabLayoutMediator: TabLayoutMediator
    private var pagerAdapterObserver: PagerAdapterObserver? = null
    private var cropActionMenu: MenuItem? = null

    private val imageDataList = listOf(
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/10/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/10/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/20/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/20/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/30/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/30/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/40/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/40/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/50/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/50/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/60/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/60/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/70/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/70/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/80/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/80/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/90/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/90/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/100/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/100/400/400.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/110/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/110/1000/1000.jpg",
            outputFileExtension = "jpg"
        ),
        ImageData(
            lowResImageUrl = "https://i.picsum.photos/id/120/200/200.jpg",
            highResImageUrl = "https://i.picsum.photos/id/120/400/400.jpg",
            outputFileExtension = "jpg"
        )
    )

    companion object {
        const val ARG_LOW_RES_IMAGE_URL = "arg_low_res_image_url"
        const val ARG_HIGH_RES_IMAGE_URL = "arg_high_res_image_url"
        const val ARG_OUTPUT_FILE_EXTENSION = "arg_output_file_extension"
        const val PREVIEW_IMAGE_REDUCED_SIZE_FACTOR = 0.1
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
    }

    private fun initializeViewPager() {
        val previewImageAdapter = PreviewImageAdapter(
            loadIntoImageViewWithResultListener = { imageData, imageView, position ->
                loadIntoImageViewWithResultListener(imageData, imageView, position)
            }
        )
        previewImageViewPager.adapter = previewImageAdapter

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
        }
    }

    private fun initializeViewModels(nonNullIntent: Intent) {
        viewModel = ViewModelProvider(this).get(PreviewImageViewModel::class.java)
        setupObservers()
        // TODO: replace dummy list
        viewModel.onCreateView(imageDataList)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this, Observer { state ->
            (previewImageViewPager.adapter as PreviewImageAdapter).submitList(state.viewPagerItemsStates)
        })

        /*viewModel.loadIntoFile.observe(this, Observer { fileState ->
            if (fileState is ImageStartLoadingToFileState) {
                loadIntoFile(fileState.imageUrl)
            }
        })

        viewModel.navigateToCropScreenWithFileInfo.observe(this, Observer { filePath ->
            navigateToCropScreenWithInputFilePath(filePath)
        })*/
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

    private fun loadIntoFile(url: String) {
        ImageEditor.instance.loadIntoFileWithResultListener(
            url,
            object : RequestListener<File> {
                override fun onResourceReady(resource: File, url: String) {
//                    viewModel.onLoadIntoFileSuccess(resource.path)
                }

                override fun onLoadFailed(e: Exception?, url: String) {
//                    viewModel.onLoadIntoFileFailed()
                }
            }
        )
    }

    private fun navigateToCropScreenWithInputFilePath(fileInfo: Pair<String, String?>) {
        // TODO: temporarily stop navigation to next screen
//        val inputFilePath = fileInfo.first
//        val outputFileExtension = fileInfo.second
//        findNavController().navigate(
//            PreviewImageFragmentDirections.actionPreviewFragmentToCropFragment(inputFilePath, outputFileExtension)
//        )
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
    }
}
