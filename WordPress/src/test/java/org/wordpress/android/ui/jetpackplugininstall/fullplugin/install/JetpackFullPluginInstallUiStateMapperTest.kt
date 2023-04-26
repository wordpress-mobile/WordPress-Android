package org.wordpress.android.ui.jetpackplugininstall.fullplugin.install

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R

class JetpackFullPluginInstallUiStateMapperTest {
    private val classToTest = JetpackFullPluginInstallUiStateMapper()

    @Test
    fun `Should map Initial state correctly`() {
        val actual = classToTest.mapInitial()
        with(actual) {
            assertThat(buttonText).isEqualTo(R.string.jetpack_plugin_install_initial_button)
            assertThat(toolbarTitle).isEqualTo(R.string.jetpack)
            assertThat(image).isEqualTo(R.drawable.ic_jetpack_logo_green_24dp)
            assertThat(imageContentDescription).isEqualTo(
                R.string.jetpack_plugin_install_jp_logo_content_description
            )
            assertThat(title).isEqualTo(R.string.jetpack_plugin_install_initial_title)
            assertThat(description).isEqualTo(R.string.jetpack_plugin_install_initial_description)
            assertThat(showCloseButton).isTrue
        }
    }

    @Test
    fun `Should map Installing state correctly`() {
        val actual = classToTest.mapInstalling()
        with(actual) {
            assertThat(toolbarTitle).isEqualTo(R.string.jetpack)
            assertThat(image).isEqualTo(R.drawable.ic_jetpack_logo_green_24dp)
            assertThat(imageContentDescription).isEqualTo(
                R.string.jetpack_plugin_install_jp_logo_content_description
            )
            assertThat(title).isEqualTo(R.string.jetpack_plugin_install_installing_title)
            assertThat(description).isEqualTo(R.string.jetpack_plugin_install_installing_description)
            assertThat(showCloseButton).isFalse
        }
    }

    @Test
    fun `Should map Done state correctly`() {
        val actual = classToTest.mapDone()
        with(actual) {
            assertThat(buttonText).isEqualTo(R.string.jetpack_plugin_install_full_plugin_done_button)
            assertThat(toolbarTitle).isEqualTo(R.string.jetpack)
            assertThat(image).isEqualTo(R.drawable.ic_jetpack_logo_green_24dp)
            assertThat(imageContentDescription).isEqualTo(
                R.string.jetpack_plugin_install_jp_logo_content_description
            )
            assertThat(title).isEqualTo(R.string.jetpack_plugin_install_done_title)
            assertThat(description).isEqualTo(R.string.jetpack_plugin_install_full_plugin_done_description)
            assertThat(showCloseButton).isFalse
        }
    }

    @Test
    fun `Should map Error state correctly`() {
        val actual = classToTest.mapError()
        with(actual) {
            assertThat(retryButtonText).isEqualTo(R.string.jetpack_plugin_install_error_button_retry)
            assertThat(contactSupportButtonText).isEqualTo(
                R.string.jetpack_plugin_install_error_button_contact_support
            )
            assertThat(toolbarTitle).isEqualTo(R.string.jetpack)
            assertThat(image).isEqualTo(R.drawable.ic_warning)
            assertThat(imageContentDescription).isEqualTo(
                R.string.jetpack_plugin_install_error_image_content_description
            )
            assertThat(title).isEqualTo(R.string.jetpack_plugin_install_error_title)
            assertThat(description).isEqualTo(R.string.jetpack_plugin_install_error_description)
            assertThat(showCloseButton).isTrue
        }
    }
}
