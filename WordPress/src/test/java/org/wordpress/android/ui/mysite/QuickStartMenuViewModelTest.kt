package org.wordpress.android.ui.mysite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Hide
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Pin
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Remove

@RunWith(MockitoJUnitRunner::class)
class QuickStartMenuViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: QuickStartMenuViewModel
    private lateinit var interactions: MutableList<QuickStartMenuInteraction>

    @Before
    fun setUp() {
        interactions = mutableListOf()

        viewModel = QuickStartMenuViewModel()
        viewModel.onInteraction.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                interactions.add(it)
            }
        }
    }

    @Test
    fun `when pin button is pressed, should emit Pin interaction event with correct id`() {
        viewModel.id = DEFAULT_ID
        viewModel.onPinActionClicked()

        assertThat(interactions).containsOnly(Pin(DEFAULT_ID))
    }

    @Test
    fun `when hide button is pressed, should emit Hide interaction event with correct id`() {
        viewModel.id = DEFAULT_ID
        viewModel.onHideActionClicked()

        assertThat(interactions).containsOnly(Hide(DEFAULT_ID))
    }

    @Test
    fun `when don't show again button is pressed, should emit Remove interaction event with correct id`() {
        viewModel.id = DEFAULT_ID
        viewModel.onRemoveActionClicked()

        assertThat(interactions).containsOnly(Remove(DEFAULT_ID))
    }

    @Test
    fun `when id changes, should emit interaction event with updated id`() {
        viewModel.id = DEFAULT_ID
        viewModel.onPinActionClicked()
        viewModel.id = CHANGED_ID
        viewModel.onPinActionClicked()

        assertThat(interactions).containsExactly(Pin(DEFAULT_ID), Pin(CHANGED_ID))
    }

    @Test
    fun `when no id is set, should not emit any interaction events`() {
        viewModel.onPinActionClicked()

        assertThat(interactions).isEmpty()
    }

    companion object {
        const val DEFAULT_ID = "default_quick_start_card_id"
        const val CHANGED_ID = "changed_quick_start_card_id"
    }
}
