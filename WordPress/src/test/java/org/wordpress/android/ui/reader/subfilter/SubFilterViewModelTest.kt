package org.wordpress.android.ui.reader.subfilter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.Organization.NO_ORGANIZATION
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.tracker.ReaderTracker

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SubFilterViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    /**
     * First tag for which the card was shown.
     */
    @Mock private lateinit var savedTag: ReaderTag
    @Mock private lateinit var subfilterListItemMapper: SubfilterListItemMapper
    @Mock private lateinit var readerTracker: ReaderTracker

    private lateinit var viewModel: SubFilterViewModel

    @Before
    fun setUp() {
        whenever(savedTag.tagTitle).thenReturn("tag-title")
        val tag = Tag(
                tag = savedTag,
                onClickAction = ::onClickActionDummy,
                organization = NO_ORGANIZATION
        )

        viewModel = SubFilterViewModel(
                readerTracker,
                subfilterListItemMapper
        )

        viewModel.start(savedTag, NO_ORGANIZATION, tag)
    }

    @Test
    fun `view model returns default filter on start`() {
        assertThat(viewModel.getCurrentSubfilterValue(NO_ORGANIZATION)).isInstanceOf(Tag::class.java)
    }

    private fun onClickActionDummy(filter: SubfilterListItem) {
        return
    }
}
