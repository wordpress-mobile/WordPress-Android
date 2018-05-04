package org.wordpress.android.viewmodel.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.ui.activitylog.ActivityLogDetailModel
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ActivityLogDetailViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var observer: Observer<ActivityLogDetailModel>
    private lateinit var viewModel: ActivityLogDetailViewModel

    private val activityID = "id1"
    private val summary = "Jetpack"
    private val text = "Blog post 123 created"
    private val actorRole = "Administrator"
    private val actorName = "John Smith"
    private val actorIcon = "image.jpg"

    private val activityLogModel = ActivityLogModel(
            activityID = activityID,
            actor = ActivityLogModel.ActivityActor(
                    displayName = actorName,
                    avatarURL = actorIcon,
                    role = actorRole,
                    type = null,
                    wpcomUserID = null
            ),
            type = "Type",
            text = text,
            summary = summary,
            gridicon = null,
            name = null,
            published = Date(10),
            rewindable = false,
            rewindID = null,
            status = null
    )

    @Before
    fun setUp() {
        viewModel = ActivityLogDetailViewModel(dispatcher, activityLogStore)
        viewModel.activityLogItem.observeForever(observer)
    }

    @Test
    fun emitsUIModelOnStart() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel))

        viewModel.start(site, activityID)

        verify(observer).onChanged(eq(ActivityLogDetailModel(
                activityID = activityID,
                summary = summary,
                text = text,
                rewindAction = null,
                actorRole = actorRole,
                actorName = actorName,
                actorIconUrl = actorIcon,
                createdDate = "January 1, 1970",
                createdTime = "1:00 AM"
        )))
    }

    @Test
    fun doesNotReemitUIModelOnStartWithTheSameActivityID() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel))

        viewModel.start(site, activityID)

        reset(observer)

        viewModel.start(site, activityID)

        verify(observer, never()).onChanged(any())
    }

    @Test
    fun emitsNewActivityOnDifferentActivityID() {
        val changedText = "new text"
        val activityID2 = "id2"
        val secondActivity = activityLogModel.copy(activityID = activityID2, text = changedText)
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel, secondActivity))

        viewModel.start(site, activityID)

        reset(observer)

        viewModel.start(site, activityID2)

        verify(observer).onChanged(eq(ActivityLogDetailModel(
                activityID = activityID2,
                summary = summary,
                text = changedText,
                rewindAction = null,
                actorRole = actorRole,
                actorName = actorName,
                actorIconUrl = actorIcon,
                createdDate = "January 1, 1970",
                createdTime = "1:00 AM"
        )))
    }

    @Test
    fun emitsNullWhenActivityNotFound() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf())

        viewModel.start(site, activityID)

        verify(observer).onChanged(null)
    }
}
