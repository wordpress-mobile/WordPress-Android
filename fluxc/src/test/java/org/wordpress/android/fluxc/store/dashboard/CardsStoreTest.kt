package org.wordpress.android.fluxc.store.dashboard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardsMapper
import org.wordpress.android.fluxc.model.dashboard.CardsModel
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class CardsStoreTest {
    @Mock private lateinit var siteModel: SiteModel

    private lateinit var cardsStore: CardsStore

    @Before
    fun setUp() {
        cardsStore = CardsStore(
                CardsMapper(),
                initCoroutineEngine()
        )
    }

    @Test
    fun `skeleton test`() = test {
        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isEqualTo(
                CardsModel(
                        posts = CardsModel.PostsModel(
                                hasPublished = false,
                                draft = listOf(),
                                scheduled = listOf()
                        )
                )
        )
    }
}
