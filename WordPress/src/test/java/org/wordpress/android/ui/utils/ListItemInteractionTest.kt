package org.wordpress.android.ui.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ListItemInteractionTest {
    @Test
    fun `triggers click without params`() {
        var clicked = false

        val interaction = ListItemInteraction.create { clicked = true }

        interaction.click()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `triggers click with params`() {
        val param = "param"
        var invokedWithParam: String? = null

        val interaction = ListItemInteraction.create(param) { invokedWithParam = it }

        interaction.click()

        assertThat(invokedWithParam).isEqualTo(param)
    }

    @Test
    fun `not equals when using lambda`() {
        val interaction1 = ListItemInteraction.create { emptyFunction() }
        val interaction2 = ListItemInteraction.create { emptyFunction() }

        assertThat(interaction1).isNotEqualTo(interaction2)
    }

    @Test
    fun `equals when using function pointers`() {
        val interaction1 = ListItemInteraction.create(this::emptyFunction)
        val interaction2 = ListItemInteraction.create(this::emptyFunction)

        assertThat(interaction1).isEqualTo(interaction2)
    }

    @Test
    fun `not equals when using lambda with a parameter`() {
        val param = "param"
        val interaction1 = ListItemInteraction.create { parametrizedEmptyFunction(param) }
        val interaction2 = ListItemInteraction.create { parametrizedEmptyFunction(param) }

        assertThat(interaction1).isNotEqualTo(interaction2)
    }

    @Test
    fun `equals when using function pointers with parameter`() {
        val param = "param"
        val interaction1 = ListItemInteraction.create(param, this::parametrizedEmptyFunction)
        val interaction2 = ListItemInteraction.create(param, this::parametrizedEmptyFunction)

        assertThat(interaction1).isEqualTo(interaction2)
    }

    private fun emptyFunction() = Unit

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun parametrizedEmptyFunction(param: String) = Unit
}
