package org.wordpress.android.ui.mysite.cards.siteinfo

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteDialogModel
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteInfoHeaderCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var wpMediaUtilsWrapper: WPMediaUtilsWrapper

    @Mock
    lateinit var mediaUtilsWrapper: MediaUtilsWrapper

    @Mock
    lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper

    @Mock
    lateinit var contextProvider: ContextProvider

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder

    private lateinit var viewModelSlice: SiteInfoHeaderCardViewModelSlice

    private lateinit var site: SiteModel

    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<MySiteViewModel.TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var uiModels: MutableList<SiteInfoHeaderCard?>

    private val siteLocalId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"

    private val activeTask = MutableLiveData<QuickStartStore.QuickStartTask>()

    @Mock
    lateinit var quickStartType: QuickStartType

    @Mock
    lateinit var siteModel: SiteModel
    @Before
    fun setUp() {
        whenever(quickStartRepository.activeTask).thenReturn(activeTask)
        whenever(selectedSiteRepository.showSiteIconProgressBar).thenReturn(MutableLiveData(false))
        whenever(selectedSiteRepository.selectedSiteChange).thenReturn(MutableLiveData(siteModel))

        viewModelSlice = SiteInfoHeaderCardViewModelSlice(
            testDispatcher(),
            quickStartRepository,
            selectedSiteRepository,
            analyticsTrackerWrapper,
            networkUtilsWrapper,
            wpMediaUtilsWrapper,
            mediaUtilsWrapper,
            fluxCUtilsWrapper,
            contextProvider,
            siteInfoHeaderCardBuilder
        )
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        uiModels = mutableListOf()

        viewModelSlice.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
            }
        }
        viewModelSlice.onTextInputDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                textInputDialogModels.add(it)
            }
        }
        viewModelSlice.onBasicDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                dialogModels.add(it)
            }
        }
        viewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        viewModelSlice.uiModel.observeForever {
            uiModels.add(it)
        }

        site = SiteModel()
        site.id = siteLocalId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        site.siteId = siteLocalId.toLong()

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_VIEW_SITE_LABEL))
            .thenReturn(QuickStartStore.QuickStartNewSiteTask.VIEW_SITE)

        viewModelSlice.initialize(testScope())
    }

    /* SITE INFO CARD */

    @Test
    fun `site info card title click shows snackbar message when network not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        Assertions.assertThat(textInputDialogModels).isEmpty()
        Assertions.assertThat(snackbars).containsOnly(
            SnackbarMessageHolder(UiString.UiStringRes(R.string.error_network_connection))
        )
    }

    @Test
    fun `site info card title click shows snackbar message when hasCapabilityManageOptions is false`() = test {
        site.hasCapabilityManageOptions = false
        site.origin = SiteModel.ORIGIN_WPCOM_REST

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        Assertions.assertThat(textInputDialogModels).isEmpty()
        Assertions.assertThat(snackbars).containsOnly(
            SnackbarMessageHolder(
                UiString.UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint)
            )
        )
    }

    @Test
    fun `site info card title click shows snackbar message when origin not ORIGIN_WPCOM_REST`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        Assertions.assertThat(textInputDialogModels).isEmpty()
        Assertions.assertThat(snackbars).containsOnly(
            SnackbarMessageHolder(UiString.UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
        )
    }

    @Test
    fun `site info card title click shows input dialog when editing allowed`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        Assertions.assertThat(snackbars).isEmpty()
        Assertions.assertThat(textInputDialogModels.last()).isEqualTo(
            MySiteViewModel.TextInputDialogModel(
                callbackId = MySiteViewModel.SITE_NAME_CHANGE_CALLBACK_ID,
                title = R.string.my_site_title_changer_dialog_title,
                initialText = siteName,
                hint = R.string.my_site_title_changer_dialog_hint,
                isMultiline = false,
                isInputEnabled = true
            )
        )
    }

    @Test
    fun `site info card icon click shows change icon dialog when site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = siteIcon

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        Assertions.assertThat(dialogModels.last()).isEqualTo(SiteDialogModel.ChangeSiteIconDialogModel)
    }

    @Test
    fun `site info card icon click shows add icon dialog when site doesn't have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = null

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        Assertions.assertThat(dialogModels.last()).isEqualTo(SiteDialogModel.AddSiteIconDialogModel)
    }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site doesn't have Jetpack`() =
        test {
            site.hasCapabilityManageOptions = true
            site.hasCapabilityUploadFiles = false
            site.setIsWPCom(false)

            invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

            Assertions.assertThat(dialogModels).isEmpty()
            Assertions.assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(
                    UiString.UiStringRes(R.string.my_site_icon_dialog_change_requires_jetpack_message)
                )
            )
        }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = siteIcon

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        Assertions.assertThat(dialogModels).isEmpty()
        Assertions.assertThat(snackbars).containsOnly(
            SnackbarMessageHolder(UiString.UiStringRes(R.string.my_site_icon_dialog_change_requires_permission_message))
        )
    }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site does not have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = null

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        Assertions.assertThat(dialogModels).isEmpty()
        Assertions.assertThat(snackbars).containsOnly(
            SnackbarMessageHolder(UiString.UiStringRes(R.string.my_site_icon_dialog_add_requires_permission_message))
        )
    }

    @Test
    fun `on site name chosen updates title if network available `() = test {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModelSlice.onSiteNameChosen(title)

        verify(selectedSiteRepository).updateTitle(title)
    }

    @Test
    fun `on site name chosen shows snackbar if network not available `() = test {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModelSlice.onSiteNameChosen(title)

        verify(selectedSiteRepository, never()).updateTitle(any())
        Assertions.assertThat(snackbars)
            .containsOnly(SnackbarMessageHolder(UiString.UiStringRes(R.string.error_update_site_title_network)))
    }

    @Test
    fun `given new site QS View Site task, when site info url clicked, site opened + View Site task completed`() =
        test {
            whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_VIEW_SITE_LABEL))
                .thenReturn(QuickStartStore.QuickStartNewSiteTask.VIEW_SITE)
            invokeSiteInfoCardAction(SiteInfoHeaderCardAction.URL_CLICK)

            verify(quickStartRepository).completeTask(QuickStartStore.QuickStartNewSiteTask.VIEW_SITE)
            Assertions.assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenSite(site))
        }

    @Test
    fun `given existing site QS View Site task, when site info url clicked, site opened + View Site task completed`() =
        test {
            whenever(quickStartType.getTaskFromString(QuickStartStore.QUICK_START_VIEW_SITE_LABEL))
                .thenReturn(QuickStartStore.QuickStartExistingSiteTask.VIEW_SITE)
            invokeSiteInfoCardAction(SiteInfoHeaderCardAction.URL_CLICK)

            verify(quickStartRepository).completeTask(QuickStartStore.QuickStartExistingSiteTask.VIEW_SITE)
            Assertions.assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenSite(site))
        }

    @Test
    fun `site info card switch click opens site picker`() = test {
        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.SWITCH_SITE_CLICK)

        Assertions.assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenSitePicker(site))
    }

    private suspend fun invokeSiteInfoCardAction(action: SiteInfoHeaderCardAction) {
        val siteInfoCard = viewModelSlice.getParams(
            site,
            activeTask.value,
            false
        )
        when (action) {
            SiteInfoHeaderCardAction.TITLE_CLICK -> siteInfoCard.titleClick()
            SiteInfoHeaderCardAction.ICON_CLICK -> siteInfoCard.iconClick()
            SiteInfoHeaderCardAction.URL_CLICK -> siteInfoCard.urlClick()
            SiteInfoHeaderCardAction.SWITCH_SITE_CLICK -> siteInfoCard.switchSiteClick()
        }
    }

    /* ADD SITE ICON DIALOG */

    @Test
    fun `when add site icon dialog +ve btn is clicked, then upload site icon task marked complete without refresh`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Positive(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).completeTask(task = QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
    }

    @Test
    fun `when change site icon dialog +ve btn clicked, then upload site icon task marked complete without refresh`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Positive(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).completeTask(task = QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
    }

    @Test
    fun `when add site icon dialog -ve btn is clicked, then upload site icon task marked complete without refresh`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Negative(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).completeTask(task = QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
    }

    @Test
    fun `when change site icon dialog -ve btn is clicked, then upload site icon task marked complete no refresh`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Negative(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).completeTask(task = QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
    }

    @Test
    fun `when site icon dialog is dismissed, then upload site icon task is marked complete without refresh`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Dismissed(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).completeTask(task = QuickStartStore.QuickStartNewSiteTask.UPLOAD_SITE_ICON)
    }

    @Test
    fun `when add site icon dialog positive button is clicked, then media picker is opened`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Positive(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG)
        )

        Assertions.assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenMediaPicker(site))
    }

    @Test
    fun `when change site icon dialog positive button is clicked, then media picker is opened`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Positive(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG)
        )

        Assertions.assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenMediaPicker(site))
    }

    @Test
    fun `when add site icon dialog negative button is clicked, then check and show quick start notice`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Negative(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when change site icon dialog negative button is clicked, then check and show quick start notice`() {
        viewModelSlice.onDialogInteraction(
                BasicDialogViewModel.DialogInteraction.Negative(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG)
            )

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when add site icon dialog is dismissed, then check and show quick start notice`() {
        viewModelSlice.onDialogInteraction(
            BasicDialogViewModel.DialogInteraction.Dismissed(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG)
        )

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when change site icon dialog is dismissed, then check and show quick start notice`() {
        viewModelSlice
            .onDialogInteraction(
                BasicDialogViewModel
                    .DialogInteraction
                    .Dismissed(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG)
            )

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when site chooser is dismissed, then check and show quick start notice`() {
        viewModelSlice.onSiteNameChooserDismissed()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when selectedSite is not null, then card is built`() {
        val siteModel = mock<SiteModel>().apply {
            id = 1
            name = "name"
            url = "https://site.wordpress.com"
        }

        clearInvocations(siteInfoHeaderCardBuilder)

        viewModelSlice.buildCard(siteModel)

        verify(siteInfoHeaderCardBuilder).buildSiteInfoCard(any())
    }

    private enum class SiteInfoHeaderCardAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }
}
