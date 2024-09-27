package org.wordpress.android.ui.selfhostedusers

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat

@AndroidEntryPoint
class UserListActivity : LocaleAwareActivity() {
    private val viewModel by viewModels<UserListViewModel>()
    private var site: SiteModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        site = if (savedInstanceState == null) {
            intent.getSerializableExtraCompat(WordPress.SITE)
        } else {
            savedInstanceState.getSerializableCompat(WordPress.SITE)
        }
        if (site == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            finish()
            return
        }

        setContentView(
            ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    UserListScreen(
                        viewModel.users.collectAsState(),
                        viewModel.progressDialogState.collectAsState(),
                        onCloseClick = {
                            viewModel.onCloseClick(this@UserListActivity)
                        },
                        onUserClick = { user ->
                            viewModel.onUserClick(this@UserListActivity, site!!,  user)
                        }
                    )
                }
            }
        )

        viewModel.fetchUsers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
    }
}
