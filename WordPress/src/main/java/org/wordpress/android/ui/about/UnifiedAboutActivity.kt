package org.wordpress.android.ui.about

import android.os.Bundle
import com.automattic.about.model.AboutConfigProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.about.UnifiedAboutNavigationAction.Dismiss
import org.wordpress.android.ui.about.UnifiedAboutNavigationAction.OpenBlog
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class UnifiedAboutActivity : LocaleAwareActivity(), AboutConfigProvider {
    @Inject
    lateinit var viewModel: UnifiedAboutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.unified_about_activity)

        viewModel.onNavigation.observeEvent(this) {
            when (it) {
                is Dismiss -> finish()
                is OpenBlog -> ActivityLauncher.openUrlExternal(this, it.url)
            }
        }
    }

    override fun getAboutConfig() = viewModel.getAboutConfig()
}
