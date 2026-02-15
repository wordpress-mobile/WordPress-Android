package org.wordpress.android.ui.newstats.components

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardPositionTest {
    @Test
    fun `when card is at index 0, then isFirst is true`() {
        val position = CardPosition(index = 0, totalCards = 3)

        assertThat(position.isFirst).isTrue()
    }

    @Test
    fun `when card is not at index 0, then isFirst is false`() {
        val position = CardPosition(index = 1, totalCards = 3)

        assertThat(position.isFirst).isFalse()
    }

    @Test
    fun `when card is at last index, then isLast is true`() {
        val position = CardPosition(index = 2, totalCards = 3)

        assertThat(position.isLast).isTrue()
    }

    @Test
    fun `when card is not at last index, then isLast is false`() {
        val position = CardPosition(index = 1, totalCards = 3)

        assertThat(position.isLast).isFalse()
    }

    @Test
    fun `when card is first, then canMoveUp is false`() {
        val position = CardPosition(index = 0, totalCards = 3)

        assertThat(position.canMoveUp).isFalse()
    }

    @Test
    fun `when card is not first, then canMoveUp is true`() {
        val position = CardPosition(index = 1, totalCards = 3)

        assertThat(position.canMoveUp).isTrue()
    }

    @Test
    fun `when card is last, then canMoveDown is false`() {
        val position = CardPosition(index = 2, totalCards = 3)

        assertThat(position.canMoveDown).isFalse()
    }

    @Test
    fun `when card is not last, then canMoveDown is true`() {
        val position = CardPosition(index = 1, totalCards = 3)

        assertThat(position.canMoveDown).isTrue()
    }

    @Test
    fun `when only one card exists, then both canMoveUp and canMoveDown are false`() {
        val position = CardPosition(index = 0, totalCards = 1)

        assertThat(position.canMoveUp).isFalse()
        assertThat(position.canMoveDown).isFalse()
    }

    @Test
    fun `when middle card in list of 3, then can move both up and down`() {
        val position = CardPosition(index = 1, totalCards = 3)

        assertThat(position.canMoveUp).isTrue()
        assertThat(position.canMoveDown).isTrue()
    }

    @Test
    fun `when only 2 cards exist, then showMoveToTopBottom is false`() {
        val position = CardPosition(index = 0, totalCards = 2)

        assertThat(position.showMoveToTopBottom).isFalse()
    }

    @Test
    fun `when 3 or more cards exist, then showMoveToTopBottom is true`() {
        val position = CardPosition(index = 0, totalCards = 3)

        assertThat(position.showMoveToTopBottom).isTrue()
    }
}
