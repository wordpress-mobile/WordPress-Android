package org.wordpress.android.ui.reader

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderUserListViewModel
import org.wordpress.android.ui.reader.views.compose.likers.ReaderUserListScreen
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

/**
 * Displays the users who like a Reader post or comment
 */
@AndroidEntryPoint
class ReaderUserListActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var readerTracker: ReaderTracker

    private val viewModel: ReaderUserListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppThemeM3 {
                ReaderUserListScreen(
                    uiState = uiState,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onLikerClick = { liker ->
                        ReaderActivityLauncher.showReaderBlogPreview(
                            this,
                            liker.blogId,
                            ReaderBlogTable.isFollowedBlog(liker.blogId),
                            ReaderTracker.SOURCE_USER,
                            readerTracker
                        )
                    }
                )
            }
        }
    }
}
