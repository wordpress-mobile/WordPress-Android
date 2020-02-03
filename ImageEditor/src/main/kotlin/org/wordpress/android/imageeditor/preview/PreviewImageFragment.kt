package org.wordpress.android.imageeditor.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.R

class PreviewImageFragment : Fragment() {
    private lateinit var viewModel: PreviewImageViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_preview_image, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PreviewImageViewModel::class.java)

        val bundle = requireActivity().intent?.extras
        val imageUrl = bundle?.getString(ARG_IMAGE_URL)
    }

    companion object {
        const val ARG_IMAGE_URL = "arg_image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            ImageEditor.dummyImageEditorSingleton.load("https://www.w3schools.com/w3css/img_snowtops.jpg")
        }
    }
}
