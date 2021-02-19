package org.wordpress.android.viewmodel.activitylog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailModel
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents
import org.wordpress.android.util.config.BackupDownloadFeatureConfig
import org.wordpress.android.util.config.RestoreFeatureConfig
import org.wordpress.android.viewmodel.Event
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ActivityLogDetailViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var activityLogStore: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var restoreFeatureConfig: RestoreFeatureConfig
    @Mock private lateinit var backupDownloadFeatureConfig: BackupDownloadFeatureConfig
    private lateinit var viewModel: ActivityLogDetailViewModel

    private val areButtonsVisible = true

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
            content = FormattableContent(text = text),
            summary = summary,
            gridicon = null,
            name = null,
            published = Date(10),
            rewindable = false,
            rewindID = null,
            status = null
    )

    private var lastEmittedItem: ActivityLogDetailModel? = null
    private var restoreVisible: Boolean = false
    private var downloadBackupVisible: Boolean = false
    private var navigationEvents: MutableList<Event<ActivityLogDetailNavigationEvents?>> = mutableListOf()

    @Before
    fun setUp() {
        viewModel = ActivityLogDetailViewModel(
                dispatcher,
                activityLogStore,
                restoreFeatureConfig,
                backupDownloadFeatureConfig
        )
        viewModel.activityLogItem.observeForever { lastEmittedItem = it }
        viewModel.restoreVisible.observeForever { restoreVisible = it }
        viewModel.downloadBackupVisible.observeForever { downloadBackupVisible = it }
        viewModel.navigationEvents.observeForever { navigationEvents.add(it) }
    }

    @After
    fun tearDown() {
        lastEmittedItem = null
    }

    @Test
    fun `given buttons are not visible, when view model starts, then restore button is not shown`() {
        viewModel.start(site, activityID, false)

        assertEquals(false, restoreVisible)
    }

    @Test
    fun `given buttons are visible, when view model starts, then restore button is shown`() {
        viewModel.start(site, activityID, true)

        assertEquals(true, restoreVisible)
    }

    @Test
    fun `given buttons are not visible, when view model starts, then download backup button is not shown`() {
        viewModel.start(site, activityID, false)

        assertEquals(false, downloadBackupVisible)
    }

    @Test
    fun `given backup download feature is disabled, when view model starts, then download backup button is shown`() {
        whenever(backupDownloadFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.start(site, activityID, true)

        assertEquals(false, downloadBackupVisible)
    }

    @Test
    fun `given backup download feature is enabled, when view model starts, then download backup button is shown`() {
        whenever(backupDownloadFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.start(site, activityID, true)

        assertEquals(true, downloadBackupVisible)
    }

    @Test
    fun emitsUIModelOnStart() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel))

        viewModel.start(site, activityID, areButtonsVisible)

        assertNotNull(lastEmittedItem)
        lastEmittedItem?.let {
            assertEquals(it.activityID, activityID)
        }
    }

    @Test
    fun showsJetpackIconWhenActorIconEmptyAndNameIsJetpackAndTypeIsApplication() {
        val updatedActivity = activityLogModel.copy(
                actor = activityLogModel.actor?.copy(
                        avatarURL = null,
                        displayName = "Jetpack",
                        type = "Application"
                )
        )
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(updatedActivity))

        viewModel.start(site, activityID, areButtonsVisible)

        assertNotNull(lastEmittedItem)
        lastEmittedItem?.let {
            assertEquals(it.activityID, activityID)
            assertEquals(it.showJetpackIcon, true)
        }
    }

    @Test
    fun showsJetpackIconWhenActorIconEmptyAndNameAndTypeIsHappinessEngineer() {
        val updatedActivity = activityLogModel.copy(
                actor = activityLogModel.actor?.copy(
                        avatarURL = null,
                        displayName = "Happiness Engineer",
                        type = "Happiness Engineer"
                )
        )
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(updatedActivity))

        viewModel.start(site, activityID, areButtonsVisible)

        assertNotNull(lastEmittedItem)
        lastEmittedItem?.let {
            assertEquals(it.activityID, activityID)
            assertEquals(it.showJetpackIcon, true)
        }
    }

    @Test
    fun doesNotReemitUIModelOnStartWithTheSameActivityID() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel))

        viewModel.start(site, activityID, areButtonsVisible)

        lastEmittedItem = null

        viewModel.start(site, activityID, areButtonsVisible)

        assertNull(lastEmittedItem)
    }

    @Test
    fun emitsNewActivityOnDifferentActivityID() {
        val changedText = "new text"
        val activityID2 = "id2"
        val updatedContent = FormattableContent(text = changedText)
        val secondActivity = activityLogModel.copy(activityID = activityID2, content = updatedContent)
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel, secondActivity))

        viewModel.start(site, activityID, areButtonsVisible)

        lastEmittedItem = null

        viewModel.start(site, activityID2, areButtonsVisible)

        assertNotNull(lastEmittedItem)
        lastEmittedItem?.let {
            assertEquals(it.activityID, activityID2)
            assertEquals(it.content, updatedContent)
        }
    }

    @Test
    fun emitsNullWhenActivityNotFound() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf())

        lastEmittedItem = mock()

        viewModel.start(site, activityID, areButtonsVisible)

        assertNull(lastEmittedItem)
    }

    @Test
    fun onRangeClickPassesClickToCLickHandler() {
        val range = mock<FormattableRange>()

        viewModel.onRangeClicked(range)

        assertEquals(range, viewModel.handleFormattableRangeClick.value)
    }

    @Test
    fun `given without rewind id, when on rewind clicked, then do nothing`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn(null)

        viewModel.onRewindClicked(model)

        assertTrue(navigationEvents.isEmpty())
    }

    @Test
    fun `given restore feature is disabled, when on rewind clicked, then show rewind dialog with model`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn("123")
        whenever(restoreFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.onRewindClicked(model)

        navigationEvents.last().peekContent()?.let {
            assertEquals(model, (it as ActivityLogDetailNavigationEvents.ShowRewindDialog).model)
        }
    }

    @Test
    fun `given restore feature is enabled, when on rewind clicked, then show restore with model`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn("123")
        whenever(restoreFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.onRewindClicked(model)

        navigationEvents.last().peekContent()?.let {
            assertEquals(model, (it as ActivityLogDetailNavigationEvents.ShowRestore).model)
        }
    }

    @Test
    fun `given without rewind id, when on download backup clicked, then do nothing`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn(null)

        viewModel.onDownloadBackupClicked(model)

        assertTrue(navigationEvents.isEmpty())
    }

    @Test
    fun `given with rewind id, when on download backup clicked, then show backup download with model`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn("123")

        viewModel.onDownloadBackupClicked(model)

        navigationEvents.last().peekContent()?.let {
            assertEquals(model, (it as ActivityLogDetailNavigationEvents.ShowBackupDownload).model)
        }
    }
}
