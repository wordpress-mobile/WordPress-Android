package org.wordpress.android.ui.reader.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_OFF
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_ON
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction.DELETE
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction.NEW
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject

/**
 * This class handles reader notification events.
 */
class ReaderSiteNotificationsUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper
) {
    fun toggleNotification(blogId: Long) {
        if (readerBlogTableWrapper.isNotificationsEnabled(blogId)) {
            analyticsUtilsWrapper.trackWithSiteId(
                    FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_OFF,
                    blogId
            )
            readerBlogTableWrapper.setNotificationsEnabledByBlogId(blogId, false)
            updateSubscription(DELETE, blogId)
        } else {
            analyticsUtilsWrapper.trackWithSiteId(
                    FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_ON,
                    blogId
            )
            readerBlogTableWrapper.setNotificationsEnabledByBlogId(blogId, true)
            updateSubscription(NEW, blogId)
        }
    }

    private fun updateSubscription(action: SubscriptionAction, blogId: Long) {
        val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
        dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(payload))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        if (event.isError) {
            AppLog.e(
                    API,
                    ReaderSiteNotificationsUseCase::class.java.simpleName + ".onSubscriptionUpdated: " +
                            event.error.type + " - " + event.error.message
            )
        } else {
            dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
        }
    }
}
