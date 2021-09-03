package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VersionUtilsTest {
    @Test
    fun `checkMinimalVersion returns true when the major part of the version is higher than the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "6.0",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion returns true when the minor part of the version is higher than the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.6",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion returns true when the patch part of the version is higher than the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5.1",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion returns true when the version is equal to the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion returns false when the major part of the version is lower than the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "4.0",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isFalse
    }

    @Test
    fun `checkMinimalVersion returns false when the minor part of the version is lower than the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.4",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isFalse
    }

    @Test
    fun `checkMinimalVersion returns false when the patch part of the version is lower than the minimal version`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5",
                minVersion = "5.5.1"
        )
        assertThat(hasMinimalVersion).isFalse
    }

    @Test
    fun `checkMinimalVersion ignores suffixes on the version string`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5-beta1",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion ignores suffixes on the minimal version string`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5-beta1",
                minVersion = "5.5-beta2"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion ignores double suffixes on the version string`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5-alpha-51379",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion ignores double suffixes on the minimal version string`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5-alpha-51370",
                minVersion = "5.5-alpha-51380"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion ignores zero on the patch part of the version string`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5.0",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion ignores zero on the patch part of the minimal version string`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5",
                minVersion = "5.5.0"
        )
        assertThat(hasMinimalVersion).isTrue
    }

    @Test
    fun `checkMinimalVersion returns false when the version string is null`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = null,
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isFalse
    }

    @Test
    fun `checkMinimalVersion returns false when the version string is empty`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "",
                minVersion = "5.5"
        )
        assertThat(hasMinimalVersion).isFalse
    }

    @Test
    fun `checkMinimalVersion returns false when the minimal version string is null`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5",
                minVersion = null
        )
        assertThat(hasMinimalVersion).isFalse
    }

    @Test
    fun `checkMinimalVersion returns false when the minimal version string is empty`() {
        val hasMinimalVersion = VersionUtils.checkMinimalVersion(
                version = "5.5",
                minVersion = ""
        )
        assertThat(hasMinimalVersion).isFalse
    }
}
