package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeStatusErrorType
import org.wordpress.android.fluxc.store.blaze.BlazeStore
import org.wordpress.android.fluxc.store.blaze.BlazeStore.BlazeStatusResult
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.PromoteWithBlazeUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.blaze.PromoteWithBlazeCardSource
import org.wordpress.android.util.config.BlazeFeatureConfig

/* SITE */

const val SITE_LOCAL_ID = 1
const val SITE_ID = 1L
const val IS_ELIGIBLE = true

/* MODEL */
private val BLAZE_STATUS_MODEL = BlazeStatusModel(siteId = SITE_ID, IS_ELIGIBLE)

private val BLAZE_STATUS_MODELS: List<BlazeStatusModel> = listOf(BLAZE_STATUS_MODEL)


@ExperimentalCoroutinesApi
class PromoteWithBlazeCardSourceTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var blazeStore: BlazeStore

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var blazeFeatureConfig: BlazeFeatureConfig
    private lateinit var blazeCardSource: PromoteWithBlazeCardSource

    private val data = BlazeStatusResult(
        model = BLAZE_STATUS_MODELS
    )

    @Suppress("UnusedPrivateMember")
    private val apiError = BlazeStatusResult<List<BlazeStatusModel>>(
        error = BlazeStatusError(BlazeStatusErrorType.API_ERROR)
    )


    @Before
    fun setUp() {
        init(true)
    }

    private fun init(isBlazeEnabled: Boolean = false) {
        setUpMocks(isBlazeEnabled)
        blazeCardSource = PromoteWithBlazeCardSource(
            selectedSiteRepository,
            blazeStore,
            blazeFeatureConfig,
            testDispatcher()
        )
    }

    @Test
    fun `given blaze is enabled, when build is invoked, then start collecting status from store (database)`() = test {
        init(true)
        blazeCardSource.refresh.observeForever { }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(blazeStore).getBlazeStatus(siteModel.siteId)
    }

    @Test
    fun `given blaze is disabled, when build is invoked, then do not collect status from store (database)`() = test {
        init(false)
        blazeCardSource.refresh.observeForever { }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verifyNoInteractions(blazeStore)
    }

    @Test
    fun `given blaze is enabled, when build is invoked, then blaze status is fetched from store (network)`() = test {
        init(true)
        whenever(blazeStore.getBlazeStatus(siteModel.siteId)).thenReturn(
            flowOf(
                BlazeStatusResult(
                    BLAZE_STATUS_MODELS
                ),
            ),
        )

        blazeCardSource.refresh.observeForever { }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever { }

        verify(blazeStore).getBlazeStatus(siteModel.siteId)
    }

    @Test
    fun `given blaze is enabled and no error, when build is invoked, the data is only loaded from get (db)`() = test {
        init(true)
        val result = mutableListOf<PromoteWithBlazeUpdate>()
        whenever(blazeStore.getBlazeStatus(siteModel.siteId)).thenReturn(flowOf(data))
        whenever(blazeStore.fetchBlazeStatus(siteModel)).thenReturn(BlazeStatusResult())
        blazeCardSource.refresh.observeForever { }

        blazeCardSource.build(testScope(), SITE_LOCAL_ID).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(PromoteWithBlazeUpdate(data.model?.first()))
    }

    @Test
    fun `given blaze is enabled, when build is invoked on site not eligible, then model is null`() = test {
        init(true)
        val invalidSiteLocalId = 2
        val result = mutableListOf<PromoteWithBlazeUpdate>()
        blazeCardSource.refresh.observeForever { }

        blazeCardSource.build(testScope(), invalidSiteLocalId).observeForever {
            it?.let { result.add(it) }
        }

        assertThat(result.size).isEqualTo(1)
        assertThat(result.first()).isEqualTo(PromoteWithBlazeUpdate(blazeStatusModel = null))
    }

    @Test
    fun `given error, when build is invoked, then model is null`() = test {
        init(true)
        val invalidSiteId = 2
        val result = mutableListOf<PromoteWithBlazeUpdate>()
        blazeCardSource.refresh.observeForever { }

        blazeCardSource.build(testScope(), invalidSiteId).observeForever {
            it?.let { result.add(it) }
        }
        advanceUntilIdle()

        assertThat(result.first()).isEqualTo(PromoteWithBlazeUpdate(blazeStatusModel = null))
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
        whenever(blazeStore.getBlazeStatus(siteModel.siteId)).thenReturn(flowOf(data))
        whenever(blazeStore.fetchBlazeStatus(siteModel)).thenReturn(BlazeStatusResult())
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
        whenever(blazeFeatureConfig.isEnabled()).thenReturn(isBlazeEnabled)
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
    }
}
