package org.wordpress.android.ui.newstats.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsFormatterTest {
    @Test
    fun `formatStatValue Long below 1000 returns raw number`() {
        assertThat(formatStatValue(500L)).isEqualTo("500")
    }

    @Test
    fun `formatStatValue Long zero returns zero`() {
        assertThat(formatStatValue(0L)).isEqualTo("0")
    }

    @Test
    fun `formatStatValue Long thousands returns K suffix`() {
        assertThat(formatStatValue(1500L)).isEqualTo("1.5K")
    }

    @Test
    fun `formatStatValue Long exact thousand returns K suffix`() {
        assertThat(formatStatValue(1000L)).isEqualTo("1.0K")
    }

    @Test
    fun `formatStatValue Long millions returns M suffix`() {
        assertThat(formatStatValue(2500000L)).isEqualTo("2.5M")
    }

    @Test
    fun `formatStatValue Long exact million returns M suffix`() {
        assertThat(formatStatValue(1000000L)).isEqualTo("1.0M")
    }

    @Test
    fun `formatStatValue Double whole number returns no decimals`() {
        assertThat(formatStatValue(5.0)).isEqualTo("5")
    }

    @Test
    fun `formatStatValue Double zero returns zero`() {
        assertThat(formatStatValue(0.0)).isEqualTo("0")
    }

    @Test
    fun `formatStatValue Double fractional returns one decimal`() {
        assertThat(formatStatValue(5.5)).isEqualTo("5.5")
    }

    @Test
    fun `formatStatValue Double negative value formats correctly`() {
        assertThat(formatStatValue(-3.7)).isEqualTo("-3.7")
    }

    @Test
    fun `formatStatValue Double large value formats correctly`() {
        assertThat(formatStatValue(1234.5)).isEqualTo("1234.5")
    }

    @Test
    fun `formatStatValue Double negative whole number returns no decimals`() {
        assertThat(formatStatValue(-2.0)).isEqualTo("-2")
    }
}
