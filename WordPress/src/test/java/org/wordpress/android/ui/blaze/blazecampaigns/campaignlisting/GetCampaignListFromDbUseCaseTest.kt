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
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetCampaignListFromDbUseCaseTest: BaseUnitTest() {
    @Mock
    lateinit var store: BlazeCampaignsStore

    @Mock
    lateinit var mapper: CampaignListingUIModelMapper

    private lateinit var getCampaignListFromDbUseCase: GetCampaignListFromDbUseCase

    @Before
    fun setUp() {
        getCampaignListFromDbUseCase = GetCampaignListFromDbUseCase(store, mapper)
    }

    @Test
    fun `given store returns empty campaigns, when usecase execute, returns no campaigns error`() = runTest {
        val siteModel = mock<SiteModel>()
        whenever(store.getBlazeCampaigns(siteModel)).thenReturn(emptyList())

        val actualResult = getCampaignListFromDbUseCase.execute(siteModel)

        assertThat(actualResult is Result.Failure).isTrue
        assertThat((actualResult as Result.Failure).value).isEqualTo(NoCampaigns)
    }

    @Test
    fun `given store returns campaigns, when usecase execute, returns campaigns `() = runTest {
        val siteModel = mock<SiteModel>()
        whenever(store.getBlazeCampaigns(siteModel)).thenReturn(mock())
        whenever(mapper.mapToCampaignModels(any())).thenReturn(mock())

        val actualResult = getCampaignListFromDbUseCase.execute(siteModel)

        assertThat(actualResult is Result.Success).isTrue
    }
}
