package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.OFF
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.ONE_HOUR
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class PublishNotificationReceiverViewModelTest {
    @Mock lateinit var postSchedulingNotificationStore: PostSchedulingNotificationStore
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: PublishNotificationReceiverViewModel
    private val notificationId = 3
    private val postId = 5

    @Before
    fun setUp() {
        viewModel = PublishNotificationReceiverViewModel(postSchedulingNotificationStore, postStore, resourceProvider)
    }

    @Test
    fun `loadNotification returns correct data for ONE_HOUR when post is scheduled`() {
        whenever(postSchedulingNotificationStore.getSchedulingReminder(notificationId)).thenReturn(
                SchedulingReminderModel(notificationId, postId, ONE_HOUR)
        )
        val post = PostModel()
        post.setStatus(PostStatus.PUBLISHED.toString())
        val postTitle = "Post title"
        post.setTitle(postTitle)
        val now = Calendar.getInstance()
        now.add(Calendar.DAY_OF_MONTH, 10)
        post.setDateCreated(DateTimeUtils.iso8601FromDate(now.time))
        whenever(postStore.getPostByLocalPostId(postId)).thenReturn(post)

        val expectedTitle = "Notification title"
        val expectedMessage = "Notification message"
        whenever(resourceProvider.getString(R.string.notification_scheduled_post_one_hour_reminder)).thenReturn(
                expectedTitle
        )
        whenever(
                resourceProvider.getString(
                        R.string.notification_post_will_be_published_in_one_hour,
                        postTitle
                )
        ).thenReturn(
                expectedMessage
        )

        val uiModel = viewModel.loadNotification(notificationId)

        assertThat(uiModel).isNotNull()
        assertThat(uiModel!!.title).isEqualTo(expectedTitle)
        assertThat(uiModel.message).isEqualTo(expectedMessage)
    }

    @Test
    fun `loadNotification returns null for OFF type`() {
        whenever(postSchedulingNotificationStore.getSchedulingReminder(notificationId)).thenReturn(
                SchedulingReminderModel(notificationId, postId, OFF)
        )
        val post = PostModel()
        post.setStatus(PostStatus.PUBLISHED.toString())
        val postTitle = "Post title"
        post.setTitle(postTitle)
        val now = Calendar.getInstance()
        now.add(Calendar.DAY_OF_MONTH, 10)
        post.setDateCreated(DateTimeUtils.iso8601FromDate(now.time))
        whenever(postStore.getPostByLocalPostId(postId)).thenReturn(post)

        val uiModel = viewModel.loadNotification(notificationId)

        assertThat(uiModel).isNull()
    }

    @Test
    fun `loadNotification returns null when post is already published`() {
        whenever(postSchedulingNotificationStore.getSchedulingReminder(notificationId)).thenReturn(
                SchedulingReminderModel(notificationId, postId, ONE_HOUR)
        )
        val post = PostModel()
        post.setStatus(PostStatus.PUBLISHED.toString())
        val postTitle = "Post title"
        post.setTitle(postTitle)
        val now = Calendar.getInstance()
        now.add(Calendar.DAY_OF_MONTH, -10)
        post.setDateCreated(DateTimeUtils.iso8601FromDate(now.time))
        whenever(postStore.getPostByLocalPostId(postId)).thenReturn(post)

        val uiModel = viewModel.loadNotification(notificationId)

        assertThat(uiModel).isNull()
    }
}
