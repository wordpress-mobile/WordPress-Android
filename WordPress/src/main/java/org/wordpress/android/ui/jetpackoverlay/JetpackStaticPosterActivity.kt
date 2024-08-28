package org.wordpress.android.ui.jetpackoverlay

import android.os.Bundle
import android.view.View.generateViewId
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.main.jetpack.staticposter.JetpackStaticPosterFragment
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackStaticPosterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { ComposeFrame() }
    }

    @Composable
    fun ComposeFrame() {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    id = generateViewId()
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { fragment ->
                supportFragmentManager.commit {
                    replace(
                        fragment.id,
                        JetpackStaticPosterFragment.newInstance(JetpackPoweredScreen.WithStaticPoster.STATS)
                    )
                }
            }
        )
    }
}
