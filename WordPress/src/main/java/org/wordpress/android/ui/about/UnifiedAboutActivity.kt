package org.wordpress.android.ui.about

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import com.automattic.about.model.AboutConfigProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.about.UnifiedAboutNavigationAction.Dismiss
import org.wordpress.android.ui.about.UnifiedAboutNavigationAction.OpenBlog
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedAboutActivity : BaseAppCompatActivity(), AboutConfigProvider {
    @Inject
    lateinit var viewModel: UnifiedAboutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.unified_about_activity)
        viewModel.onNavigation.observeEvent(this) { action -> 
            when (action) {
                is Dismiss -> finish()
                is OpenBlog -> openExternalBlogUrl(action.url)
            }
        }
    }

    private fun openExternalBlogUrl(url: String){
        try{
            ActivityLauncher.openUrlExternal(this, url)
        }catch(e: ActivityNotFoundException){
            Toast.makeText(this, "External Activity does not exists or is invalid.", Toast.LENGTH_SHORT).show()
        }
    }
    override fun getAboutConfig() = viewModel.getAboutConfig()
}
