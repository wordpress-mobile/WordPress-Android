package org.wordpress.android.imageeditor.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar

import androidx.fragment.app.Fragment
import org.wordpress.android.imageeditor.R

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource

import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class MainImageFragment : Fragment() {
    private var contentUri: String? = null
    private var fragmentWasPaused: Boolean = false

    private var imageView: ImageView? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        contentUri = args?.getString(ARG_MEDIA_CONTENT_URI)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.main_image_fragment, container, false)
        imageView = view.findViewById(R.id.main_image)
        progressBar = view.findViewById(R.id.progress_loading)

        return view
    }

    override fun onPause() {
        fragmentWasPaused = true

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (fragmentWasPaused) {
            fragmentWasPaused = false
        } else {
            loadImage(contentUri)
        }
    }

    /*
     * loads and displays a remote or local image
     */
    private fun loadImage(mediaUri: String?) {
        imageView?.let {
            it.visibility = View.VISIBLE

            // TODO
            Glide.with(this)
                    .load(mediaUri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            progressBar?.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            progressBar?.visibility = View.GONE
                            return false
                        }
                    })
                    .into(it)
        }
    }

    companion object {
        const val TAG = "main_image_fragment"

        const val ARG_MEDIA_CONTENT_URI = "content_uri"
        const val ARG_MEDIA_ID = "media_id"

        /**
         * @param contentUri URI of media - can be local or remote
         */
        fun newInstance(
            contentUri: String
        ): MainImageFragment {
            val args = Bundle()
            args.putString(ARG_MEDIA_CONTENT_URI, contentUri)

            val fragment = MainImageFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
