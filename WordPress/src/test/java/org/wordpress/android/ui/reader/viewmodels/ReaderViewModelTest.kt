package org.wordpress.android.ui.reader.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.LoadReaderItemsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.utils.ReaderTopBarMenuHelper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig
import java.util.Date

private const val DUMMY_CURRENT_TIME: Long = 10000000000

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReaderViewModel

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var dateProvider: DateProvider

    @Mock
    lateinit var loadReaderItemsUseCase: LoadReaderItemsUseCase

    @Mock
    lateinit var readerTracker: ReaderTracker

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    lateinit var readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig

    private val urlUtilsWrapper = UrlUtilsWrapper()

    @Before
    fun setup() {
        viewModel = ReaderViewModel(
            testDispatcher(),
            testDispatcher(),
            appPrefsWrapper,
            dateProvider,
            loadReaderItemsUseCase,
            readerTracker,
            accountStore,
            jetpackBrandingUtils,
            jetpackFeatureRemovalOverlayUtil,
            ReaderTopBarMenuHelper(readerTagsFeedFeatureConfig),
            urlUtilsWrapper,
            readerTagsFeedFeatureConfig,
        )

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
    }

    @Test
    fun `updateTags invoked on reader tab content is first displayed`() = test {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(-1)
        whenever(loadReaderItemsUseCase.load()).thenReturn(ReaderTagList())
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `updateTags NOT invoked if lastUpdate within threshold`() = test {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD + 1)
        whenever(loadReaderItemsUseCase.load()).thenReturn(ReaderTagList())
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNull()
    }

    @Test
    fun `updateTags invoked if lastUpdate NOT within threshold`() = test {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD - 1)
        whenever(loadReaderItemsUseCase.load()).thenReturn(ReaderTagList())
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    private companion object {
        private const val UPDATE_TAGS_THRESHOLD: Long = 1000 * 60 * 60 // 1 hour in milliseconds
    }
}
