package org.wordpress.android.imageeditor.preview

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_preview_image.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.RequestListener
import org.wordpress.android.imageeditor.R.layout
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState
import org.wordpress.android.imageeditor.utils.UiHelpers

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel

    companion object {
        const val ARG_LOW_RES_IMAGE_URL = "arg_low_res_image_url"
        const val ARG_HIGH_RES_IMAGE_URL = "arg_high_res_image_url"
        const val PREVIEW_IMAGE_REDUCED_SIZE_FACTOR = 0.1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layout.fragment_preview_image, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullIntent = checkNotNull(requireActivity().intent)
        initializeViewModels(nonNullIntent)
    }

    private fun initializeViewModels(nonNullIntent: Intent) {
        val lowResImageUrl = nonNullIntent.getStringExtra(ARG_LOW_RES_IMAGE_URL)
        val highResImageUrl = nonNullIntent.getStringExtra(ARG_HIGH_RES_IMAGE_URL)

        viewModel = ViewModelProvider(this).get(PreviewImageViewModel::class.java)
        setupObservers()
        viewModel.start(lowResImageUrl, highResImageUrl)
    }

    private fun setupObservers() {
        viewModel.loadImageFromData.observe(this, Observer {
            loadImage(it)
        })
    }

    private fun loadImage(imageData: ImageData) {
        ImageEditor.instance.loadImageWithResultListener(
            imageData.highResImageUrl,
            previewImageView,
            CENTER,
            imageData.lowResImageUrl,
            object : RequestListener<Drawable> {
                override fun onResourceReady(resource: Drawable) {
                    updateViews(ImageUiState.ImageLoadSuccessContentUiState) // TODO: Fix progress bar hiding on low res image load
                }

                override fun onLoadFailed(e: Exception?) {
                    updateViews(ImageUiState.ImageLoadFailedContentUiState)
                }
            }
        )
    }

    private fun updateViews(state: ImageUiState) {
        UiHelpers.updateVisibility(progressBar, state.progressBarVisible)
    }
}
