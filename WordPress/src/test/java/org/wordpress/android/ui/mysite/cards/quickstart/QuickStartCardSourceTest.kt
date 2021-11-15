package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate

class QuickStartCardSourceTest : BaseUnitTest() {
    @Mock lateinit var quickStartRepository: QuickStartRepository
    private lateinit var quickStartCardSource: QuickStartCardSource
    private val quickStartUpdate = MutableLiveData(QuickStartUpdate())
    private val siteLocalId: Int = 1

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        whenever(quickStartRepository.getQuickStartUpdate(any(), any())).thenReturn(quickStartUpdate)
        quickStartCardSource = QuickStartCardSource(quickStartRepository)
    }

    @Test
    fun `when source is requested upon start, then quick start update is empty`() = test {
        var result: QuickStartUpdate? = null
        quickStartCardSource.buildSource(testScope(), siteLocalId).observeForever { it?.let { result = it } }

        Assertions.assertThat(result?.categories).isEmpty()
        Assertions.assertThat(result?.activeTask).isNull()
    }
}
