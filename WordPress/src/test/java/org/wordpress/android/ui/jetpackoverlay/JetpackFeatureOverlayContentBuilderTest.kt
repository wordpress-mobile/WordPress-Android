package org.wordpress.android.ui.jetpackoverlay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString.UiStringRes

/**
 * The two overlay states used to be assembled per removal phase. They are now fixed, so these tests
 * pin the content each one produces — in particular the component visibility, which is the only
 * thing that still differs between them.
 */
class JetpackFeatureOverlayContentBuilderTest {
    private val builder = JetpackFeatureOverlayContentBuilder()

    @Test
    fun `feature collection overlay cannot be dismissed with the close button`() {
        val state = builder.buildFeatureCollectionOverlayState(isRtl = false)

        assertThat(state.componentVisibility.closeButton).isFalse
        assertThat(state.componentVisibility.secondaryButton).isTrue
        assertThat(state.componentVisibility.migrationText).isTrue
    }

    @Test
    fun `feature collection overlay shows the self-hosted content`() {
        val state = builder.buildFeatureCollectionOverlayState(isRtl = false)

        assertThat(state.overlayContent.title)
            .isEqualTo(R.string.wp_jetpack_feature_removal_phase_self_hosted_users_title)
        assertThat(state.overlayContent.caption)
            .isEqualTo(UiStringRes(R.string.wp_jetpack_feature_removal_phase_self_hosted_users_description))
        assertThat(state.overlayContent.migrationText)
            .isEqualTo(R.string.wp_jetpack_feature_removal_overlay_migration_helper_text)
    }

    @Test
    fun `deep link overlay keeps the close button and has no migration text`() {
        val state = builder.buildDeepLinkOverlayState(isRtl = false)

        assertThat(state.componentVisibility.closeButton).isTrue
        assertThat(state.componentVisibility.migrationText).isFalse
        assertThat(state.overlayContent.migrationText).isNull()
    }

    @Test
    fun `deep link overlay offers both apps`() {
        val state = builder.buildDeepLinkOverlayState(isRtl = false)

        assertThat(state.overlayContent.title).isEqualTo(R.string.wp_jetpack_deep_link_overlay_title)
        assertThat(state.overlayContent.primaryButtonText).isEqualTo(R.string.wp_jetpack_deep_link_open_in_jetpack)
        assertThat(state.overlayContent.secondaryButtonText).isEqualTo(R.string.wp_jetpack_deep_link_open_in_wordpress)
    }

    @Test
    fun `illustration follows the layout direction`() {
        assertThat(builder.buildFeatureCollectionOverlayState(isRtl = true).overlayContent.illustration)
            .isEqualTo(R.raw.wp2jp_rtl)
        assertThat(builder.buildFeatureCollectionOverlayState(isRtl = false).overlayContent.illustration)
            .isEqualTo(R.raw.wp2jp_left)
        assertThat(builder.buildDeepLinkOverlayState(isRtl = true).overlayContent.illustration)
            .isEqualTo(R.raw.wp2jp_rtl)
        assertThat(builder.buildDeepLinkOverlayState(isRtl = false).overlayContent.illustration)
            .isEqualTo(R.raw.wp2jp_left)
    }
}
