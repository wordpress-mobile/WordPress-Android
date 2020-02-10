package org.wordpress.android.imageeditor.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_preview_image.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.R.layout

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel
    private val imageEditor = ImageEditor.instance

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
        val lowResImageUrl = nonNullIntent.getStringExtra(ARG_LOW_RES_IMAGE_URL) as String

        initializeViewModels()
        loadImage(lowResImageUrl)
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProvider(this).get(PreviewImageViewModel::class.java)
    }

    private fun loadImage(@NonNull imageUrl: String) {
        imageEditor.loadUrlIntoImageView(imageUrl, previewImageView, CENTER)
    }
}
