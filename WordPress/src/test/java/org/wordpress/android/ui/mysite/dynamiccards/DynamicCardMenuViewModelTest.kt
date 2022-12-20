package org.wordpress.android.ui.mysite.dynamiccards

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Hide
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Pin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Remove
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Unpin

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class DynamicCardMenuViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: DynamicCardMenuViewModel
    private lateinit var mInteractions: MutableList<DynamicCardMenuInteraction>

    @Before
    fun setUp() {
        mInteractions = mutableListOf()

        viewModel = DynamicCardMenuViewModel()
        viewModel.onInteraction.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                mInteractions.add(it)
            }
        }
    }

    @Test
    fun `when pin button is pressed, should emit Pin interaction event with correct id and isPinned is false`() {
        viewModel.start(CUSTOMIZE_QUICK_START.name, false)
        viewModel.onPinActionClicked()

        assertThat(mInteractions).containsOnly(Pin(CUSTOMIZE_QUICK_START))
    }

    @Test
    fun `when pin button is pressed, should emit Unpin interaction event when isPinned is true`() {
        viewModel.start(CUSTOMIZE_QUICK_START.name, true)
        viewModel.onPinActionClicked()

        assertThat(mInteractions).containsOnly(Unpin(CUSTOMIZE_QUICK_START))
    }

    @Test
    fun `when hide button is pressed, should emit Hide interaction event with correct id`() {
        viewModel.start(CUSTOMIZE_QUICK_START.name, false)
        viewModel.onHideActionClicked()

        assertThat(mInteractions).containsOnly(Hide(CUSTOMIZE_QUICK_START))
    }

    @Test
    fun `when don't show again button is pressed, should emit Remove interaction event with correct id`() {
        viewModel.start(CUSTOMIZE_QUICK_START.name, false)
        viewModel.onRemoveActionClicked()

        assertThat(mInteractions).containsOnly(Remove(CUSTOMIZE_QUICK_START))
    }

    @Test
    fun `when id changes, should emit interaction event with updated id`() {
        viewModel.start(CUSTOMIZE_QUICK_START.name, false)
        viewModel.onPinActionClicked()
        viewModel.start(GROW_QUICK_START.name, false)
        viewModel.onPinActionClicked()

        assertThat(mInteractions).containsExactly(Pin(CUSTOMIZE_QUICK_START), Pin(GROW_QUICK_START))
    }
}
