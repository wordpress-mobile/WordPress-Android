package org.wordpress.android.fluxc.media

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.media.MediaErrorSubType
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.NO_ERROR
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.UNSUPPORTED_MIME_TYPE
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.UndefinedSubType

@RunWith(MockitoJUnitRunner::class)
class MediaErrorSubTypeTest {
    @Test
    fun `deserialize returns UndefinedSubType when name is null`() {
        val result = MediaErrorSubType.deserialize(null)
        assertThat(result).isEqualTo(UndefinedSubType)
    }

    @Test
    fun `deserialize returns UndefinedSubType when name does not match any mapped subtype`() {
        val result = MediaErrorSubType.deserialize("this_does_not:match")
        assertThat(result).isEqualTo(UndefinedSubType)
    }

    @Test
    fun `deserialize returns MalformedMediaArgSubType(UNSUPPORTED_MIME_TYPE) when name matches`() {
        val result = MediaErrorSubType.deserialize("MALFORMED_MEDIA_ARG_SUBTYPE:UNSUPPORTED_MIME_TYPE")
        assertThat(result).isEqualTo(MalformedMediaArgSubType(UNSUPPORTED_MIME_TYPE))
    }

    @Test
    fun `deserialize returns NO_ERROR(null) when name matches`() {
        val result = MediaErrorSubType.deserialize("MALFORMED_MEDIA_ARG_SUBTYPE:NO_ERROR")
        assertThat(result).isEqualTo(MalformedMediaArgSubType(NO_ERROR))
    }
}
