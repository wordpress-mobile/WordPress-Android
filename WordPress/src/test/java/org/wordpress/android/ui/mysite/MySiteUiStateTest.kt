package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MySiteUiStateTest : BaseUnitTest() {
    @Mock
    lateinit var postsCardModel: PostsCardModel
    private lateinit var mySiteUiState: MySiteUiState

    @Before
    fun setUp() {
        mySiteUiState = MySiteUiState()
    }

    /* CARDS UPDATE - SNACKBAR */

    @Test
    fun `given cards update with snackbar, when ui state is updated with cards update, then snackbar shown`() {
        val partialState = CardsUpdate(cards = listOf(postsCardModel), showSnackbarError = true)

        val updatedUiState = mySiteUiState.update(partialState)

        assertThat(updatedUiState.cardsUpdate?.showSnackbarError).isTrue
    }

    @Test
    fun `given cards update with snackbar, when ui state is updated with different update, then snackbar not shown`() {
        val cardsUpdate = CardsUpdate(cards = listOf(postsCardModel), showSnackbarError = true)
        val quickStartUpdate = QuickStartUpdate()

        var updatedUiState = mySiteUiState.update(cardsUpdate)
        updatedUiState = updatedUiState.update(quickStartUpdate)

        assertThat(updatedUiState.cardsUpdate?.showSnackbarError).isFalse
    }

    /* CARDS UPDATE - STALE MESSAGE */

    @Test
    fun `given cards update with stale msg, when ui state is updated with cards update, then stale msg shown`() {
        val partialState = CardsUpdate(cards = listOf(postsCardModel), showStaleMessage = true)

        val updatedUiState = mySiteUiState.update(partialState)

        assertThat(updatedUiState.cardsUpdate?.showStaleMessage).isTrue
    }

    @Test
    fun `given cards update with stale msg, when ui state is updated with different update, then stale msg shown`() {
        val cardsUpdate = CardsUpdate(cards = listOf(postsCardModel), showStaleMessage = true)
        val quickStartUpdate = QuickStartUpdate()

        var updatedUiState = mySiteUiState.update(cardsUpdate)
        updatedUiState = updatedUiState.update(quickStartUpdate)

        assertThat(updatedUiState.cardsUpdate?.showStaleMessage).isTrue
    }
}
