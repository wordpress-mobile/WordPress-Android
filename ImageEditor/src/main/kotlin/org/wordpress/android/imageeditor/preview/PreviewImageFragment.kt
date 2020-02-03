package org.wordpress.android.imageeditor.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_preview_image.*
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.R.layout

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel

    companion object {
        const val ARG_IMAGE_URL = "arg_image_url"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layout.fragment_preview_image, container, false)

    override fun onSaveInstanceState(outState: Bundle) {
        val intent = activity?.intent
        if (intent != null) {
            if (intent.hasExtra(ARG_IMAGE_URL)) {
                outState.putString(
                        ARG_IMAGE_URL,
                        intent.getStringExtra(ARG_IMAGE_URL)
                )
            }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(savedInstanceState)
        initializeViewModels()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val nonNullIntent = checkNotNull(requireActivity().intent)

        // TODO: replace ARG_IMAGE_URL with thumb/ low res image url
        val previewImageUrl = if (savedInstanceState == null) {
            nonNullIntent.getStringExtra(ARG_IMAGE_URL)
        } else {
            savedInstanceState.getString(ARG_IMAGE_URL)
        }
        previewImageUrl?.let {
            previewImageFromUrl(it)
        }
    }

    private fun initializeViewModels() {
        viewModel = ViewModelProviders.of(this).get(PreviewImageViewModel::class.java)
    }

    private fun previewImageFromUrl(@NonNull imageUrl: String) {
        val imageEditor = ImageEditor.instance
        imageEditor.loadUrlIntoImageView(imageUrl, previewImageView, CENTER)
    }
}
