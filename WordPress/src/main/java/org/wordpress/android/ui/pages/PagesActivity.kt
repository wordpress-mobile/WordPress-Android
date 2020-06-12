package org.wordpress.android.ui.pages

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.pages_fragment.*
import org.wordpress.android.R
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import javax.inject.Inject

const val EXTRA_PAGE_REMOTE_ID_KEY = "extra_page_remote_id_key"
const val EXTRA_PAGE_PARENT_ID_KEY = "extra_page_parent_id_key"

class PagesActivity : LocaleAwareActivity(),
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface {
    @Inject internal lateinit var systemNotificationTracker: SystemNotificationsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pages_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
            val notificationType: NotificationType =
                    intent.getSerializableExtra(ARG_NOTIFICATION_TYPE) as NotificationType
            systemNotificationTracker.trackTappedNotification(notificationType)
        }

        if (intent.hasExtra(EXTRA_PAGE_REMOTE_ID_KEY)) {
            val pageId = intent.getLongExtra(EXTRA_PAGE_REMOTE_ID_KEY, -1)
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                (it as PagesFragment).onSpecificPageRequested(pageId)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPositiveClicked(instanceTag: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PagesFragment) {
            fragment.onPositiveClickedForBasicDialog(instanceTag)
        } else {
            throw IllegalStateException("PagesFragment is required to consume this event.")
        }
    }

    override fun onNegativeClicked(instanceTag: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is PagesFragment) {
            fragment.onNegativeClickedForBasicDialog(instanceTag)
        } else {
            throw IllegalStateException("PagesFragment is required to consume this event.")
        }
    }
}
