package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.SiteSettingsInterfaceWrapper

class SelectedSiteRepositoryTest : BaseUnitTest() {
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var siteSettingsInterfaceFactory: SiteSettingsInterfaceWrapper.Factory
    @Mock lateinit var siteSettingsInterfaceWrapper: SiteSettingsInterfaceWrapper
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var siteModel: SiteModel
    private var siteIconProgressBarVisible: Boolean = false
    private var selectedSite: SiteModel? = null
    private lateinit var actions: MutableList<Action<*>>
    private lateinit var selectedSiteRepository: SelectedSiteRepository
    private var onSaveError: (() -> Unit)? = null
    private var onFetchError: (() -> Unit)? = null
    private var onSettingsSaved: (() -> Unit)? = null
    private val selectedSiteId = 1

    @Before
    fun setUp() {
        selectedSiteRepository = SelectedSiteRepository(dispatcher, siteSettingsInterfaceFactory, appPrefsWrapper)
        selectedSiteRepository.showSiteIconProgressBar.observeForever { siteIconProgressBarVisible = it == true }
        selectedSiteRepository.selectedSiteChange.observeForever { selectedSite = it }
        siteModel = SiteModel()
        siteModel.id = selectedSiteId
        whenever(siteSettingsInterfaceWrapper.localSiteId).thenReturn(selectedSiteId)
        doAnswer {
            actions.add(it.getArgument(0))
        }.whenever(dispatcher).dispatch(any())
        actions = mutableListOf()
    }

    @Test
    fun `icon update updates site settings and shows progress bar when selected site is initialized`() {
        initializeSiteAndSiteSettings()
        val mediaId = 5

        selectedSiteRepository.updateSiteIconMediaId(mediaId, true)

        assertThat(siteIconProgressBarVisible).isTrue
        val inOrder = inOrder(siteSettingsInterfaceWrapper)
        inOrder.verify(siteSettingsInterfaceWrapper).setSiteIconMediaId(mediaId)
        inOrder.verify(siteSettingsInterfaceWrapper).saveSettings()
    }

    @Test
    fun `icon update updates site settings and does not show progress bar when parameter is false`() {
        initializeSiteAndSiteSettings()
        val mediaId = 5

        selectedSiteRepository.updateSiteIconMediaId(mediaId, false)

        assertThat(siteIconProgressBarVisible).isFalse
        val inOrder = inOrder(siteSettingsInterfaceWrapper)
        inOrder.verify(siteSettingsInterfaceWrapper).setSiteIconMediaId(mediaId)
        inOrder.verify(siteSettingsInterfaceWrapper).saveSettings()
    }

    @Test
    fun `icon update does not show progress bar when site not initialized`() {
        val mediaId = 5

        selectedSiteRepository.updateSiteIconMediaId(mediaId, true)

        assertThat(siteIconProgressBarVisible).isFalse
        verifyZeroInteractions(siteSettingsInterfaceWrapper)
    }

    @Test
    fun `showSiteIconProgressBar(true) shows progress bar`() {
        selectedSiteRepository.showSiteIconProgressBar(true)

        assertThat(siteIconProgressBarVisible).isTrue
    }

    @Test
    fun `showSiteIconProgressBar(false) hides progress bar`() {
        selectedSiteRepository.showSiteIconProgressBar(false)

        assertThat(siteIconProgressBarVisible).isFalse
    }

    @Test
    fun `title update changes site name, settings title and dispatches change`() {
        initializeSiteAndSiteSettings()
        siteModel.name = "original title"
        val updatedTitle = "updated title"

        selectedSiteRepository.updateTitle(updatedTitle)

        assertThat(siteModel.name).isEqualTo(updatedTitle)
        verify(dispatcher).dispatch(any())
        assertThat(actions.last().payload).isEqualTo(siteModel)
        assertThat(actions.last().type).isEqualTo(SiteAction.UPDATE_SITE)

        val inOrder = inOrder(siteSettingsInterfaceWrapper)
        inOrder.verify(siteSettingsInterfaceWrapper).title = updatedTitle
        inOrder.verify(siteSettingsInterfaceWrapper).saveSettings()
    }

    @Test
    fun `updateSite updates site`() {
        assertThat(selectedSiteRepository.hasSelectedSite()).isFalse
        assertThat(selectedSiteRepository.getSelectedSite()).isNull()

        selectedSiteRepository.updateSite(siteModel)

        assertThat(selectedSiteRepository.hasSelectedSite()).isTrue
        assertThat(selectedSiteRepository.getSelectedSite()).isEqualTo(siteModel)
    }

    @Test
    fun `updateSite hides progress bar if icon has changed`() {
        siteModel.iconUrl = "originalIcon.jpg"
        initializeSiteAndSiteSettings()
        val updatedSite = SiteModel()
        updatedSite.iconUrl = "updatedIcon.jpg"
        selectedSiteRepository.showSiteIconProgressBar(true)

        selectedSiteRepository.updateSite(updatedSite)

        assertThat(siteIconProgressBarVisible).isFalse
    }

    @Test
    fun `updateSite does not hide progress bar if icon has not changed`() {
        val iconName = "originalIcon.jpg"
        siteModel.iconUrl = iconName
        initializeSiteAndSiteSettings()
        val updatedSite = SiteModel()
        updatedSite.iconUrl = iconName
        selectedSiteRepository.showSiteIconProgressBar(true)

        selectedSiteRepository.updateSite(updatedSite)

        assertThat(siteIconProgressBarVisible).isTrue
    }

    @Test
    fun `site settings onSaveError hides icon progress bar`() {
        initializeSiteAndSiteSettings()
        selectedSiteRepository.showSiteIconProgressBar(true)
        assertThat(siteIconProgressBarVisible).isTrue

        onSaveError!!.invoke()

        assertThat(siteIconProgressBarVisible).isFalse
    }

    @Test
    fun `site settings onFetchError hides icon progress bar`() {
        initializeSiteAndSiteSettings()
        selectedSiteRepository.showSiteIconProgressBar(true)
        assertThat(siteIconProgressBarVisible).isTrue

        onFetchError!!.invoke()

        assertThat(siteIconProgressBarVisible).isFalse
    }

    @Test
    fun `site settings onSettingsSaved emits fetch site event`() {
        initializeSiteAndSiteSettings()

        onSettingsSaved!!.invoke()

        assertThat(actions.last().type).isEqualTo(SiteAction.FETCH_SITE)
        assertThat(actions.last().payload).isEqualTo(siteModel)
    }

    @Test
    fun `clears previous site settings when selected site has different ID`() {
        val firstSiteId = 1
        val updatedSiteId = 2
        whenever(siteSettingsInterfaceWrapper.localSiteId).thenReturn(firstSiteId)
        initializeSiteAndSiteSettings()
        siteModel.id = updatedSiteId

        selectedSiteRepository.updateSiteSettingsIfNecessary()

        verify(siteSettingsInterfaceWrapper).clear()
    }

    @Test
    fun `emits null site ID when site not selected`() {
        var emptySiteIdEmitted = false

        selectedSiteRepository.siteSelected.observeForever { emptySiteIdEmitted = true }

        assertThat(emptySiteIdEmitted).isTrue
    }

    private fun initializeSiteAndSiteSettings() {
        selectedSiteRepository.updateSite(siteModel)
        doAnswer {
            onSaveError = it.getArgument(1)
            onFetchError = it.getArgument(2)
            onSettingsSaved = it.getArgument(4)
            siteSettingsInterfaceWrapper
        }.whenever(siteSettingsInterfaceFactory).build(any(), any(), any(), isNull(), any(), isNull())

        selectedSiteRepository.updateSiteSettingsIfNecessary()
        verify(siteSettingsInterfaceWrapper).init(true)
        verify(siteSettingsInterfaceFactory).build(eq(siteModel), any(), any(), isNull(), any(), isNull())
    }
}
