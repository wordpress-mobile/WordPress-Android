package org.wordpress.android.datasets

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel

class SiteSettingsProviderImplTest {
    private val provider = SiteSettingsProviderImpl()

    private fun site(
        mobileEditor: String? = null,
        isWPCom: Boolean = false,
        isWPComAtomic: Boolean = false
    ) = SiteModel().apply {
        setMobileEditor(mobileEditor)
        setIsWPCom(isWPCom)
        setIsWPComAtomic(isWPComAtomic)
    }

    @Test
    fun `null editor defaults to block editor`() {
        assertThat(
            provider.isBlockEditorDefault(site(mobileEditor = null))
        ).isTrue()
    }

    @Test
    fun `empty editor defaults to block editor`() {
        assertThat(
            provider.isBlockEditorDefault(site(mobileEditor = ""))
        ).isTrue()
    }

    @Test
    fun `gutenberg editor returns true`() {
        assertThat(
            provider.isBlockEditorDefault(
                site(mobileEditor = "gutenberg")
            )
        ).isTrue()
    }

    @Test
    fun `non-gutenberg editor on self-hosted returns false`() {
        assertThat(
            provider.isBlockEditorDefault(
                site(mobileEditor = "aztec")
            )
        ).isFalse()
    }

    @Test
    fun `non-gutenberg editor on WPCom simple returns true`() {
        assertThat(
            provider.isBlockEditorDefault(
                site(
                    mobileEditor = "aztec",
                    isWPCom = true,
                    isWPComAtomic = false
                )
            )
        ).isTrue()
    }

    @Test
    fun `non-gutenberg editor on WPCom Atomic returns false`() {
        assertThat(
            provider.isBlockEditorDefault(
                site(
                    mobileEditor = "aztec",
                    isWPCom = true,
                    isWPComAtomic = true
                )
            )
        ).isFalse()
    }
}
