package org.wordpress.android.ui.reader.comments

import android.os.Bundle
import org.wordpress.android.R
import org.wordpress.android.databinding.ThreadedCommentsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderPostPagerActivity.DirectOperation
import org.wordpress.android.ui.reader.comments.ThreadedCommentsFragment.ThreadedCommentsFragmentArgs

class ThreadedCommentsActivity : LocaleAwareActivity() {
    private var binding: ThreadedCommentsActivityBinding? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ThreadedCommentsActivityBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        val fm = supportFragmentManager
        var threadedCommentsFragment = fm.findFragmentByTag(TAG_THREADED_COMMENTS_FRAGMENT)

        if (threadedCommentsFragment == null) {
            threadedCommentsFragment = ThreadedCommentsFragment.newInstance(ThreadedCommentsFragmentArgs(
                    blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0),
                    postId = intent.getLongExtra(ReaderConstants.ARG_POST_ID, 0),
                    directOperation = (intent
                            .getSerializableExtra(ReaderConstants.ARG_DIRECT_OPERATION) as DirectOperation?),
                    commentId = intent.getLongExtra(ReaderConstants.ARG_COMMENT_ID, 0),
                    interceptedUri = intent.getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI)
            ))
            fm.beginTransaction()
                    .add(R.id.fragment_container, threadedCommentsFragment, TAG_THREADED_COMMENTS_FRAGMENT)
                    .commit()
        }
    }

    companion object {
        private const val TAG_THREADED_COMMENTS_FRAGMENT = "tag_threaded_comments_fragment"
    }
}
