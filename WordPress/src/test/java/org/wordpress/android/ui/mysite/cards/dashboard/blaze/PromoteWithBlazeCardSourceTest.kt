package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.blaze.PromoteWithBlazeCardSource

/* SITE */

const val SITE_LOCAL_ID = 1
const val SITE_ID = 1L

@ExperimentalCoroutinesApi
class PromoteWithBlazeCardSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var blazeFeatureUtils: BlazeFeatureUtils

    private lateinit var blazeCardSource: PromoteWithBlazeCardSource


    @Before
    fun setUp() {
        init(true)
    }

    private fun init(isBlazeEnabled: Boolean = false) {
        setUpMocks(isBlazeEnabled)
        blazeCardSource = PromoteWithBlazeCardSource(
            selectedSiteRepository,
            blazeFeatureUtils
        )
    }

    @Test
    fun `given blaze is enabled, when build is invoked, then card is shown`() = test {
        init(true)
        val result = mutableListOf<Boolean>()
        blazeCardSource.refresh.observeForever { result.add(it) }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        assertThat(result.last()).isTrue
    }

    @Test
    fun `given blaze is disabled, when build is invoked, then card is not shown`() = test {
        init(false)
        val result = mutableListOf<Boolean>()
        blazeCardSource.refresh.observeForever { result.add(it) }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        assertThat(result.last()).isFalse
    }

    @Test
    fun `when build is invoked, then refresh is set to true`() = test {
        init(true)
        val result = mutableListOf<Boolean>()
        blazeCardSource.refresh.observeForever { result.add(it) }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isFalse
        assertThat(result.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is set to true`() = test {
        init(true)
        val result = mutableListOf<Boolean>()
        blazeCardSource.refresh.observeForever { result.add(it) }
        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        blazeCardSource.refresh()
        advanceUntilIdle()

        assertThat(result.size).isEqualTo(5)
        assertThat(result[0]).isFalse // init
        assertThat(result[1]).isTrue // build(...) -> refresh()
        assertThat(result[2]).isTrue // build(...) -> fetch
        assertThat(result[3]).isFalse // refresh()
        assertThat(result[4]).isFalse // refreshData(...) -> fetch -> error
    }

    private fun setUpMocks(isBlazeEnabled: Boolean) {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(blazeFeatureUtils.shouldShowBlazeCardEntryPoint(siteModel)).thenReturn(isBlazeEnabled)
    }
}
