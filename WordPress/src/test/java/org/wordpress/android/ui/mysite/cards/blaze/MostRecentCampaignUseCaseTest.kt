package org.wordpress.android.ui.mysite.cards.blaze

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Result
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MostRecentCampaignUseCaseTest: BaseUnitTest() {
    @Mock
    lateinit var store: BlazeCampaignsStore

    private lateinit var mostRecentCampaignUseCase: MostRecentCampaignUseCase

    @Before
    fun setUp() {
        mostRecentCampaignUseCase = MostRecentCampaignUseCase(store)
    }

    @Test
    fun `given store returns no campaigns, when usecase execute, returns generic error`() = runTest {
        val siteModel = mock<SiteModel>()
        whenever(store.getMostRecentBlazeCampaign(siteModel)).thenReturn(null)

        val actualResult = mostRecentCampaignUseCase.execute(siteModel)

        Assertions.assertThat(actualResult is Result.Failure).isTrue
        Assertions.assertThat((actualResult as Result.Failure).value).isEqualTo(NoCampaigns)
    }


    @Test
    fun `given store returns campaigns, when usecase execute, returns campaigns`() = runTest {
        val siteModel = mock<SiteModel>()
        whenever(store.getMostRecentBlazeCampaign(siteModel)).thenReturn(mock())

        val actualResult = mostRecentCampaignUseCase.execute(siteModel)

        Assertions.assertThat(actualResult is Result.Success).isTrue
    }
}
