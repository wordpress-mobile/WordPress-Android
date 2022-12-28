package org.wordpress.android.viewmodel.activitylog

import android.text.SpannableString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRange
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailModel
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailNavigationEvents
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ActivityLogDetailViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var dispatcher: Dispatcher
    @Mock
    private lateinit var activityLogStore: ActivityLogStore
    @Mock
    private lateinit var resourceProvider: ResourceProvider
    @Mock
    private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock
    private lateinit var site: SiteModel
    private lateinit var viewModel: ActivityLogDetailViewModel

    private val areButtonsVisible = true
    private val isRestoreHidden = false

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
    private var multisiteVisible: Pair<Boolean, SpannableString?> = Pair(false, null)
    private var navigationEvents: MutableList<Event<ActivityLogDetailNavigationEvents?>> = mutableListOf()

    @Before
    fun setUp() {
        viewModel = ActivityLogDetailViewModel(
            dispatcher,
            activityLogStore,
            resourceProvider,
            htmlMessageUtils
        )
        viewModel.activityLogItem.observeForever { lastEmittedItem = it }
        viewModel.restoreVisible.observeForever { restoreVisible = it }
        viewModel.downloadBackupVisible.observeForever { downloadBackupVisible = it }
        viewModel.multisiteVisible.observeForever { multisiteVisible = it }
        viewModel.navigationEvents.observeForever { navigationEvents.add(it) }
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any())).thenReturn("")
        whenever(resourceProvider.getString(anyInt())).thenReturn("")
    }

    @After
    fun tearDown() {
        lastEmittedItem = null
    }

    @Test
    fun `given buttons not visible and restore not hidden, when view model starts, then restore button is not shown`() {
        val areButtonsVisible = false
        val isRestoreHidden = false

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertEquals(false, restoreVisible)
    }

    @Test
    fun `given button not visible and restore hidden, when view model starts, then restore button is not shown`() {
        val areButtonsVisible = false
        val isRestoreHidden = true

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertEquals(false, restoreVisible)
    }

    @Test
    fun `given buttons visible and restore not hidden, when view model starts, then restore button is shown`() {
        val areButtonsVisible = true
        val isRestoreHidden = false

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertEquals(true, restoreVisible)
    }

    @Test
    fun `given button visible and restore hidden, when view model starts, then restore button is not shown`() {
        val areButtonsVisible = true
        val isRestoreHidden = true

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertEquals(false, restoreVisible)
    }

    @Test
    fun `given buttons not visible, when view model starts, then download backup button is not shown`() {
        val areButtonsVisible = false
        val isRestoreHidden = false

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertEquals(false, downloadBackupVisible)
    }

    @Test
    fun `given buttons visible, when view model starts, then download backup button is shown`() {
        val areButtonsVisible = true
        val isRestoreHidden = false

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertEquals(true, downloadBackupVisible)
    }

    @Test
    fun `given restore not hidden, when view model starts, then multisite message is not shown`() {
        val areButtonsVisible = true
        val isRestoreHidden = false

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertFalse(multisiteVisible.first)
        assertNull(multisiteVisible.second)
    }

    @Test
    fun `given restore hidden, when view model starts, then multisite message is shown`() {
        val areButtonsVisible = true
        val isRestoreHidden = true

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertTrue(multisiteVisible.first)
        assertNotNull(multisiteVisible.second)
    }

    @Test
    fun emitsUIModelOnStart() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel))

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

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

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

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

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertNotNull(lastEmittedItem)
        lastEmittedItem?.let {
            assertEquals(it.activityID, activityID)
            assertEquals(it.showJetpackIcon, true)
        }
    }

    @Test
    fun doesNotReemitUIModelOnStartWithTheSameActivityID() {
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel))

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        lastEmittedItem = null

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertNull(lastEmittedItem)
    }

    @Test
    fun emitsNewActivityOnDifferentActivityID() {
        val changedText = "new text"
        val activityID2 = "id2"
        val updatedContent = FormattableContent(text = changedText)
        val secondActivity = activityLogModel.copy(activityID = activityID2, content = updatedContent)
        whenever(activityLogStore.getActivityLogForSite(site)).thenReturn(listOf(activityLogModel, secondActivity))

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        lastEmittedItem = null

        viewModel.start(site, activityID2, areButtonsVisible, isRestoreHidden)

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

        viewModel.start(site, activityID, areButtonsVisible, isRestoreHidden)

        assertNull(lastEmittedItem)
    }

    @Test
    fun onRangeClickPassesClickToCLickHandler() {
        val range = mock<FormattableRange>()

        viewModel.onRangeClicked(range)

        assertEquals(range, viewModel.handleFormattableRangeClick.value)
    }

    @Test
    fun `given without rewind id, when on restore clicked, then do nothing`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn(null)

        viewModel.onRestoreClicked(model)

        assertTrue(navigationEvents.isEmpty())
    }

    @Test
    fun `when on restore clicked, then show restore with model`() {
        val model = mock<ActivityLogDetailModel>()
        whenever(model.rewindId).thenReturn("123")

        viewModel.onRestoreClicked(model)

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
