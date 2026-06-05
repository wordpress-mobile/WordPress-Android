package org.wordpress.android.support.unified.util

import android.app.Application
import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.unified.model.AttachmentState

private const val MB = 1024L * 1024L

@ExperimentalCoroutinesApi
class AttachmentStateValidatorTest : BaseUnitTest() {
    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var validator: AttachmentStateValidator

    @Before
    fun setUp() {
        whenever(application.contentResolver).thenReturn(contentResolver)

        validator = AttachmentStateValidator(
            application = application,
            appLogWrapper = appLogWrapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun mockUriWithSize(sizeBytes: Long): Uri {
        val uri = mock<Uri>()
        val descriptor = mock<AssetFileDescriptor>()
        whenever(descriptor.length).thenReturn(sizeBytes)
        whenever(contentResolver.openAssetFileDescriptor(eq(uri), any())).thenReturn(descriptor)
        return uri
    }

    @Test
    fun `addAttachments adds URIs to attachment state`() = test {
        val uri1 = mockUriWithSize(1 * MB)
        val uri2 = mockUriWithSize(1 * MB)

        val state = validator.addAttachments(AttachmentState(), listOf(uri1, uri2))

        assertThat(state.acceptedUris).containsExactly(uri1, uri2)
        assertThat(state.rejectedUris).isEmpty()
    }

    @Test
    fun `addAttachments appends to existing attachments`() = test {
        val uri1 = mockUriWithSize(1 * MB)
        val uri2 = mockUriWithSize(1 * MB)
        val uri3 = mockUriWithSize(1 * MB)

        var state = validator.addAttachments(AttachmentState(), listOf(uri1))
        state = validator.addAttachments(state, listOf(uri2, uri3))

        assertThat(state.acceptedUris).containsExactly(uri1, uri2, uri3)
    }

    @Test
    fun `addAttachments rejects file larger than 20MB`() = test {
        val uri1 = mockUriWithSize(21 * MB)

        val state = validator.addAttachments(AttachmentState(), listOf(uri1))

        assertThat(state.acceptedUris).isEmpty()
        assertThat(state.rejectedUris).containsExactly(uri1)
        assertThat(state.rejectedTotalSizeBytes).isEqualTo(21 * MB)
    }

    @Test
    fun `addAttachments accepts file smaller than 20MB`() = test {
        val uri1 = mockUriWithSize(10 * MB)

        val state = validator.addAttachments(AttachmentState(), listOf(uri1))

        assertThat(state.acceptedUris).containsExactly(uri1)
        assertThat(state.rejectedUris).isEmpty()
        assertThat(state.currentTotalSizeBytes).isEqualTo(10 * MB)
    }

    @Test
    fun `addAttachments accepts file exactly at 20MB limit`() = test {
        val uri1 = mockUriWithSize(20 * MB)

        val state = validator.addAttachments(AttachmentState(), listOf(uri1))

        assertThat(state.acceptedUris).containsExactly(uri1)
        assertThat(state.rejectedUris).isEmpty()
        assertThat(state.currentTotalSizeBytes).isEqualTo(20 * MB)
    }

    @Test
    fun `addAttachments rejects files when total size exceeds 20MB`() = test {
        // Start with 12MB, then try to add 10MB (exceeds limit) and 3MB (fits)
        val uri1 = mockUriWithSize(12 * MB)
        val uri2 = mockUriWithSize(10 * MB)
        val uri3 = mockUriWithSize(3 * MB)

        var state = validator.addAttachments(AttachmentState(), listOf(uri1))
        state = validator.addAttachments(state, listOf(uri2, uri3))

        // uri1 (12MB) accepted, uri2 (10MB) rejected (12+10=22 exceeds 20MB), uri3 (3MB) accepted
        assertThat(state.acceptedUris).containsExactly(uri1, uri3)
        assertThat(state.rejectedUris).containsExactly(uri2)
        assertThat(state.currentTotalSizeBytes).isEqualTo(15 * MB)
        assertThat(state.rejectedTotalSizeBytes).isEqualTo(10 * MB)
    }

    @Test
    fun `addAttachments accepts multiple files within total size limit`() = test {
        // 12MB + 7MB = 19MB (within limit)
        val uri1 = mockUriWithSize(12 * MB)
        val uri2 = mockUriWithSize(7 * MB)

        var state = validator.addAttachments(AttachmentState(), listOf(uri1))
        state = validator.addAttachments(state, listOf(uri2))

        assertThat(state.acceptedUris).containsExactly(uri1, uri2)
        assertThat(state.rejectedUris).isEmpty()
        assertThat(state.currentTotalSizeBytes).isEqualTo(19 * MB)
    }

    @Test
    fun `addAttachments accepts file when size cannot be determined`() = test {
        val uri1 = mock<Uri>()
        whenever(contentResolver.openAssetFileDescriptor(eq(uri1), any())).thenReturn(null)

        val state = validator.addAttachments(AttachmentState(), listOf(uri1))

        assertThat(state.acceptedUris).containsExactly(uri1)
        assertThat(state.rejectedUris).isEmpty()
    }

    @Test
    fun `removeAttachment removes specific URI from attachments list`() = test {
        val uri1 = mockUriWithSize(1 * MB)
        val uri2 = mockUriWithSize(1 * MB)
        val uri3 = mockUriWithSize(1 * MB)
        val state = validator.addAttachments(AttachmentState(), listOf(uri1, uri2, uri3))

        val updated = validator.removeAttachment(state, uri2)

        assertThat(updated.acceptedUris).containsExactly(uri1, uri3)
    }

    @Test
    fun `removeAttachment does nothing when URI not in list`() = test {
        val uri1 = mockUriWithSize(1 * MB)
        val uri2 = mockUriWithSize(1 * MB)
        val uri3 = mock<Uri>()
        val state = validator.addAttachments(AttachmentState(), listOf(uri1, uri2))

        val updated = validator.removeAttachment(state, uri3)

        assertThat(updated.acceptedUris).containsExactly(uri1, uri2)
    }
}
