package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Result
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsError
import org.wordpress.android.fluxc.network.rest.wpcom.blaze.BlazeCampaignsErrorType
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchCampaignListUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var store: BlazeCampaignsStore

    @Mock
    lateinit var mapper: CampaignListingUIModelMapper

    private lateinit var fetchCampaignListUseCase: FetchCampaignListUseCase

    @Before
    fun setUp() {
        fetchCampaignListUseCase = FetchCampaignListUseCase(store, mapper)
    }

    @Test
    fun `given store returns error, when usecase execute, returns generic error`() = runTest {
        val siteModel = mock<SiteModel>()
        val offset = 0
        whenever(store.fetchBlazeCampaigns(siteModel, offset, PER_PAGE)).thenReturn(
            BlazeCampaignsStore.BlazeCampaignsResult(BlazeCampaignsError(BlazeCampaignsErrorType.INVALID_RESPONSE))
        )

        val actualResult = fetchCampaignListUseCase.execute(siteModel, offset)

        assertThat(actualResult is Result.Failure).isTrue
        assertThat((actualResult as Result.Failure).value).isEqualTo(GenericResult)
    }

    @Test
    fun `given store returns empty campaigns, when usecase execute, returns no campaigns error`() = runTest {
        val siteModel = mock<SiteModel>()
        val offset = 0
        whenever(store.fetchBlazeCampaigns(siteModel, offset, PER_PAGE)).thenReturn(
            BlazeCampaignsStore.BlazeCampaignsResult(
                BlazeCampaignsModel(campaigns = emptyList(), skipped = 0, totalItems = 1)
            )
        )

        val actualResult = fetchCampaignListUseCase.execute(siteModel, offset)

        assertThat(actualResult is Result.Failure).isTrue
        assertThat((actualResult as Result.Failure).value).isEqualTo(NoCampaigns)
    }

    @Test
    fun `given store returns campaigns, when usecase execute, returns campaigns`() = runTest {
        val siteModel = mock<SiteModel>()
        val offset = 0
        whenever(store.fetchBlazeCampaigns(siteModel, offset, PER_PAGE)).thenReturn(
            BlazeCampaignsStore.BlazeCampaignsResult(
                BlazeCampaignsModel(campaigns = mock(), skipped = 0, totalItems = 1)
            )
        )
        whenever(mapper.mapToCampaignModels(any())).thenReturn(mock())

        val actualResult = fetchCampaignListUseCase.execute(siteModel, offset)

        assertThat(actualResult is Result.Success).isTrue
    }

    companion object {
        const val PER_PAGE = 10
    }
}
