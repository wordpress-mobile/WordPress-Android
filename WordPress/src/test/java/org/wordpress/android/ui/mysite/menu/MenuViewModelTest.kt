package org.wordpress.android.ui.mysite.menu

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.JetpackMigrationLanguageUtil
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ContextProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MenuViewModelTest : BaseUnitTest() {
    private val blazeFeatureUtils: BlazeFeatureUtils = mock()
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase = mock()
    private val jetpackMigrationLanguageUtil: JetpackMigrationLanguageUtil = mock()
    private val listItemActionHandler: ListItemActionHandler = mock()
    private val localeManagerWrapper: LocaleManagerWrapper = mock()
    private val quickStartRepository: QuickStartRepository = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val siteItemsBuilder: SiteItemsBuilder = mock()
    private val refreshAppLanguageObserver: Observer<String> = mock()
    private val contextProvider: ContextProvider = mock()
    private val uiHelpers: UiHelpers = mock()

    private lateinit var viewModel: MenuViewModel

    private val site = SiteModel().apply { siteId = 123L }
    private val siteRemoteId = 123L

    @Before
    fun setUp() = test {
        whenever(localeManagerWrapper.getLanguage()).thenReturn("")
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        initJetpackCapabilities(scanPurchased = false, backupPurchased = false)

        viewModel = MenuViewModel(
            blazeFeatureUtils,
            jetpackCapabilitiesUseCase,
            jetpackMigrationLanguageUtil,
            listItemActionHandler,
            localeManagerWrapper,
            quickStartRepository,
            selectedSiteRepository,
            siteItemsBuilder,
            contextProvider,
            uiHelpers,
            testDispatcher()
        )

        viewModel.refreshAppLanguage.observeForever(refreshAppLanguageObserver)
    }

    @Test
    fun `when vm, when init, then uiState should contain empty list`() = test {
        assertThat(viewModel.uiState.first()).isEqualTo(MenuViewState(items = emptyList()))
    }

    @Test
    fun `given vm, when started, then list contains list of items`() = test {
        val uiStates = mutableListOf<MenuViewState>()
        testWithData(uiStates) {
            initJetpackCapabilities(scanPurchased = true, backupPurchased = true)

            whenever(siteItemsBuilder.build(any()))
                .thenReturn(createList(false))
                .thenReturn(createList(scanPurchased = true, backupPurchased = true))

            viewModel.start()

            advanceUntilIdle()

            assertThat((uiStates.last()).items).isNotEmpty
        }
    }

    /* ITEM VISIBILITY */
    @Test
    fun `given vm start, when jetpack capabilities excludes backup, then backup menu item is NOT visible`() = test {
        val uiStates = mutableListOf<MenuViewState>()
        testWithData(uiStates) {
            whenever(siteItemsBuilder.build(any()))
                .thenReturn(createList(scanPurchased = false, backupPurchased = false))
                .thenReturn(createList(scanPurchased = false, backupPurchased = false))

            viewModel.start()

            advanceUntilIdle()

            assertThat(findBackupListItem(uiStates.last().items)).isNull()
        }
    }

    @Test
    fun `given vm start, when jetpack capabilities includes backup, then backup menu item is visible`() = test {
        val uiStates = mutableListOf<MenuViewState>()
        testWithData(uiStates) {
            whenever(siteItemsBuilder.build(any()))
                .thenReturn(createList(scanPurchased = false, backupPurchased = false))
                .thenReturn(createList(scanPurchased = false, backupPurchased = true))

            viewModel.start()

            advanceUntilIdle()

            assertThat(findBackupListItem(uiStates.last().items)).isNotNull()
        }
    }
    @Test
    fun `given vm start, when jetpack capabilities excludes scan, then scan menu item is NOT visible`() = test {
        val uiStates = mutableListOf<MenuViewState>()
        testWithData(uiStates) {
            whenever(siteItemsBuilder.build(any()))
                .thenReturn(createList(scanPurchased = false, backupPurchased = false))
                .thenReturn(createList(scanPurchased = false, backupPurchased = false))

            viewModel.start()

            advanceUntilIdle()

            assertThat(findScanListItem(uiStates.last().items)).isNull()
        }
    }

    @Test
    fun `given vm start, when jetpack capabilities includes scan, then scan menu item is visible`() = test {
        val uiStates = mutableListOf<MenuViewState>()
        testWithData(uiStates) {
            whenever(siteItemsBuilder.build(any()))
                .thenReturn(createList(scanPurchased = false, backupPurchased = false))
                .thenReturn(createList(scanPurchased = true, backupPurchased = false))

            viewModel.start()

            advanceUntilIdle()

            assertThat(findScanListItem(uiStates.last().items)).isNotNull()
        }
    }

    private fun findBackupListItem(items: List<MenuItemState>) =
        items.filterIsInstance(MenuItemState.MenuListItem::class.java)
            .firstOrNull { it.listItemAction == ListItemAction.BACKUP }

    private fun findScanListItem(items: List<MenuItemState>) =
        items.filterIsInstance(MenuItemState.MenuListItem::class.java)
            .firstOrNull { it.listItemAction == ListItemAction.SCAN }

    private suspend fun initJetpackCapabilities(
        scanPurchased: Boolean = false,
        backupPurchased: Boolean = false
    ) {
        val products =
            JetpackCapabilitiesUseCase.JetpackPurchasedProducts(scan = scanPurchased, backup = backupPurchased)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(flowOf(products))
    }

    private fun testWithData(
        uiStates: MutableList<MenuViewState> = mutableListOf(),
        testBody: suspend TestScope.() -> Unit
    ) = test {
        val uiStatesJob = launch { viewModel.uiState.toList(uiStates) }
        testBody(testScope())
        uiStatesJob.cancel()
    }

    private fun createList(
        scanPurchased: Boolean = false,
        backupPurchased: Boolean = false
    ): List<MySiteCardAndItem.Item.ListItem> {
        val items = mutableListOf<MySiteCardAndItem.Item.ListItem>()
        items.add(
            MySiteCardAndItem.Item.ListItem(
                0,
                UiString.UiStringRes(0),
                onClick = ListItemInteraction.create(ListItemAction.POSTS, mock()),
                listItemAction = ListItemAction.POSTS
            )
        )
        if (scanPurchased) {
            items.add(
                MySiteCardAndItem.Item.ListItem(
                    0,
                    UiString.UiStringRes(0),
                    onClick = ListItemInteraction.create(ListItemAction.SCAN, mock()),
                    listItemAction = ListItemAction.SCAN
                )
            )
        }
        if (backupPurchased) {
            items.add(
                MySiteCardAndItem.Item.ListItem(
                    0,
                    UiString.UiStringRes(0),
                    onClick = ListItemInteraction.create(ListItemAction.BACKUP, mock()),
                    listItemAction = ListItemAction.BACKUP
                )
            )
        }
        return items
    }
}
