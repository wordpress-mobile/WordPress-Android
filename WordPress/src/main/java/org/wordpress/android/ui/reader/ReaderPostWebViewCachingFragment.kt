package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.views.ReaderWebView
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

/**
 * Renders a post into an off-screen WebView so its content is cached, then removes itself. Rendering happens on
 * the UI thread, so a configuration change restarts it from scratch.
 */
@AndroidEntryPoint
class ReaderPostWebViewCachingFragment : Fragment() {
    @Inject
    lateinit var readerCssProvider: ReaderCssProvider

    @Inject
    lateinit var getReadingPreferencesUseCase: ReaderGetReadingPreferencesSyncUseCase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ReaderWebView(requireActivity())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        // check the network again, to catch a disconnect during loading or a configuration change
        val post = if (NetworkUtils.isNetworkAvailable(view.context)) {
            ReaderPostTable.getBlogPost(args.getLong(ARG_BLOG_ID), args.getLong(ARG_POST_ID), false)
        } else {
            null
        }
        if (post == null) {
            removeSelf()
            return
        }

        val webView = view as ReaderWebView
        webView.isPrivatePost = post.isPrivate
        webView.blogSchemeIsHttps = UrlUtils.isHttps(post.blogUrl)
        webView.pageFinishedListener = ReaderWebView.ReaderWebViewPageFinishedListener { _, _ -> removeSelf() }
        // the WebView caches the content as it renders it
        ReaderPostRenderer(webView, post, readerCssProvider, getReadingPreferencesUseCase.invoke()).beginRender()
    }

    private fun removeSelf() {
        if (isAdded) {
            // the state of this fragment doesn't matter
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }

    companion object {
        private const val ARG_BLOG_ID = "blog_id"
        private const val ARG_POST_ID = "post_id"

        fun newInstance(blogId: Long, postId: Long) = ReaderPostWebViewCachingFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_BLOG_ID, blogId)
                putLong(ARG_POST_ID, postId)
            }
        }
    }
}
