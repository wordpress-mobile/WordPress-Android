package org.wordpress.android.ui.reader.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostBookmarkUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    lateinit var useCase: ReaderPostBookmarkUseCase
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper
    @Mock lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Before
    fun setup() {
        useCase = ReaderPostBookmarkUseCase(
                analyticsTrackerWrapper,
                TEST_DISPATCHER,
                networkUtilsWrapper,
                appPrefsWrapper,
                readerPostActionsWrapper,
                readerPostTableWrapper
        )
    }

    @Test
    fun `bookmark action updates the bookmark state to true`() = test {
        // Arrange
        val dummyPost = init(isBookmarked = false)
        // Act
        useCase.toggleBookmark(0L, 0L, false)

        // Assert
        verify(readerPostActionsWrapper).addToBookmarked(dummyPost)
    }

    @Test
    fun `unbookmark action updates the bookmark state to false`() = test {
        // Arrange
        val dummyPost = init(isBookmarked = true)
        // Act
        useCase.toggleBookmark(0L, 0L, false)

        // Assert
        verify(readerPostActionsWrapper).removeFromBookmarked(dummyPost)
    }

    @Test
    fun `initiates content preload when network available`() = test {
        // Arrange
        init(isBookmarked = false, networkAvailable = true)

        var observedValue: Event<PreLoadPostContent>? = null
        useCase.preloadPostEvents.observeForever {
            observedValue = it
        }
        // Act
        useCase.toggleBookmark(0L, 0L, false)

        // Assert
        assertThat(observedValue).isNotNull
    }

    @Test
    fun `does not initiate content preload when network not available`() = test {
        // Arrange
        init(isBookmarked = false, networkAvailable = false)

        var observedValue: Event<PreLoadPostContent>? = null
        useCase.preloadPostEvents.observeForever {
            observedValue = it
        }
        // Act
        useCase.toggleBookmark(0L, 0L, false)

        // Assert
        assertThat(observedValue).isNull()
    }

    @Test
    fun `does not initiate content preload on unbookmark action`() = test {
        // Arrange
        init(isBookmarked = true)

        var observedValue: Event<PreLoadPostContent>? = null
        useCase.preloadPostEvents.observeForever {
            observedValue = it
        }
        // Act
        useCase.toggleBookmark(0L, 0L, false)

        // Assert
        assertThat(observedValue).isNull()
    }

    @Test
    fun `does not initiate content preload when on bookmarkList(savedTab)`() = test {
        // Arrange
        init()
        var observedValue: Event<PreLoadPostContent>? = null
        useCase.preloadPostEvents.observeForever {
            observedValue = it
        }
        // Act
        useCase.toggleBookmark(0L, 0L, true)

        // Assert
        assertThat(observedValue).isNull()
    }

    @Test
    fun `shows dialog on first use`() = test {
        // Arrange
        init()
        whenever(appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()).thenReturn(true)
        var observedValue: Event<ReaderNavigationEvents>? = null
        useCase.navigationEvents.observeForever {
            observedValue = it
        }
        // Act
        useCase.toggleBookmark(0L, 0L, false)

        // Assert
        assertThat(observedValue!!.peekContent()).isInstanceOf(ShowBookmarkedSavedOnlyLocallyDialog::class.java)
    }

    private fun init(isBookmarked: Boolean = false, networkAvailable: Boolean = true): ReaderPost {
        val post = ReaderPost().apply { this.isBookmarked = isBookmarked }
        whenever(readerPostTableWrapper.getBlogPost(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(post)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(networkAvailable)
        return post
    }
}
