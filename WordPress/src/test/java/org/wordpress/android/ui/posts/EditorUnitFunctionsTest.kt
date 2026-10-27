package org.wordpress.android.ui.posts

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [EditorUnitFunctions.isMediaTypeIntent], which needs real URI parsing and a
 * real [MimeTypeMap].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class EditorUnitFunctionsTest {
    @Test
    fun `accepts a uri whose name carries a recognized extension`() {
        val intent = Intent().apply { type = "text/plain" }

        assertThat(EditorUnitFunctions.isMediaTypeIntent(intent, "file:///cache/photo.jpg".toUri())).isTrue()
    }

    @Test
    fun `rejects a uri whose extension is not media even when the intent type is`() {
        // the URI wins whenever it resolves: the intent type is only a fallback for when it doesn't
        val intent = Intent().apply { type = "image/jpeg" }

        assertThat(EditorUnitFunctions.isMediaTypeIntent(intent, "file:///cache/notes.pdf".toUri())).isFalse()
    }

    @Test
    fun `falls back to the intent type when the name contains a space`() {
        // getFileExtensionFromUrl() returns an empty string for names containing a space
        val intent = Intent().apply { type = "image/jpeg" }

        assertThat(
            EditorUnitFunctions.isMediaTypeIntent(intent, "file:///cache/Screenshot 2026-08-19.jpg".toUri())
        ).isTrue()
    }

    @Test
    fun `falls back to the intent type when the shared file has no extension`() {
        // what a provider that reports no usable display name leaves behind
        val intent = Intent().apply { type = "image/jpeg" }

        assertThat(EditorUnitFunctions.isMediaTypeIntent(intent, "file:///cache/wp-1755600000000.".toUri())).isTrue()
    }

    @Test
    fun `rejects an extension-less uri when the intent type is not media`() {
        val intent = Intent().apply { type = "text/plain" }

        assertThat(EditorUnitFunctions.isMediaTypeIntent(intent, "file:///cache/wp-1755600000000.".toUri())).isFalse()
    }

    @Test
    fun `rejects an extension-less uri when the intent has no type at all`() {
        assertThat(EditorUnitFunctions.isMediaTypeIntent(Intent(), "file:///cache/wp-1755600000000.".toUri()))
            .isFalse()
    }

    @Test
    fun `uses the intent type when no uri is given`() {
        val intent = Intent().apply { type = "video/mp4" }

        assertThat(EditorUnitFunctions.isMediaTypeIntent(intent, null)).isTrue()
    }
}
