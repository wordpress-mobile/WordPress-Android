package org.wordpress.android.ui.posts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R

/**
 * A simple [Fragment] subclass.
 */
class PrepublishingOptionsFragments : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.post_prepublishing_options_fragment, container, false)
    }

    companion object {
        const val TAG ="prepublishing_options_fragment_tag"
    }
}
