package org.wordpress.android.ui.mysite.cards

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.ui.mysite.cards.migration.JpMigrationSuccessCardViewModelSlice
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.publicdata.AppStatus
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.R
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JpMigrationSuccessCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var appStatus: AppStatus

    @Mock
    lateinit var wordPressPublicData: WordPressPublicData

    @Mock
    lateinit var contentMigrationAnalyticsTracker: ContentMigrationAnalyticsTracker

    private lateinit var viewModel: JpMigrationSuccessCardViewModelSlice
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var uiModels: MutableList<MySiteCardAndItem.Item.SingleActionCard?>

    @Before
    fun setUp() {
        viewModel = JpMigrationSuccessCardViewModelSlice(
            buildConfigWrapper,
            appPrefsWrapper,
            appStatus,
            wordPressPublicData,
            contentMigrationAnalyticsTracker
        )

        navigationActions = mutableListOf()
        uiModels = mutableListOf()

        viewModel.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        viewModel.uiModel.observeForever { event ->
            uiModels.add(event)
        }
    }

    @Test
    fun `when all conditions are met, when buildCard is invoked, then should build card`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(any())).thenReturn(true)
        whenever(wordPressPublicData.currentPackageId()).thenReturn("org.wordpress.android")

        viewModel.buildCard()

        assert(uiModels.last() is MySiteCardAndItem.Item.SingleActionCard)
        val uiModel = uiModels.last() as MySiteCardAndItem.Item.SingleActionCard

        assertEquals(R.string.jp_migration_success_card_message, uiModel.textResource)
        assertEquals(R.drawable.ic_wordpress_jetpack_appicon, uiModel.imageResource)
    }

    @Test
    fun `when is not jetpack app, when buildCard is invoked, then should not build card`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(any())).thenReturn(true)
        whenever(wordPressPublicData.currentPackageId()).thenReturn("org.wordpress.android")

        viewModel.buildCard()

        assertNull(uiModels.last())
    }

    @Test
    fun `when migration is not complete, when buildCard is invoked, then should not build card`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(false)
        whenever(appStatus.isAppInstalled(any())).thenReturn(true)
        whenever(wordPressPublicData.currentPackageId()).thenReturn("org.wordpress.android")

        viewModel.buildCard()

        assertNull(uiModels.last())
    }

    @Test
    fun `when app is not installed, when buildCard is invoked, then should not build card`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(any())).thenReturn(false)
        whenever(wordPressPublicData.currentPackageId()).thenReturn("org.notwordpress.android")

        viewModel.buildCard()

        assertNull(uiModels.last())
    }

    @Test
    fun `when card is built, when action click, then event is tracked`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(any())).thenReturn(true)
        whenever(wordPressPublicData.currentPackageId()).thenReturn("org.wordpress.android")

        viewModel.buildCard()

        val card = uiModels.last() as MySiteCardAndItem.Item.SingleActionCard
        card.onActionClick()

        verify(contentMigrationAnalyticsTracker).trackPleaseDeleteWordPressCardTapped()
        assertNotNull(navigationActions.last())
        assertEquals(SiteNavigationAction.OpenJetpackMigrationDeleteWP, navigationActions.last())
    }
}
