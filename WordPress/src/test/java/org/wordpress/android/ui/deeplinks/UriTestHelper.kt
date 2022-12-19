package org.wordpress.android.ui.deeplinks

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.UriWrapper

fun buildUri(host: String?, vararg path: String): UriWrapper {
    val uri = mock<UriWrapper>()
    if (host != null) {
        whenever(uri.host).thenReturn(host)
    }
    if (path.isNotEmpty()) {
        whenever(uri.pathSegments).thenReturn(path.toList())
    }
    return uri
}

fun buildUri(
    host: String? = null,
    queryParam1: Pair<String, String>? = null,
    queryParam2: Pair<String, String>? = null
): UriWrapper {
    val uri = mock<UriWrapper>()
    if (host != null) {
        whenever(uri.host).thenReturn(host)
    }
    if (queryParam1 != null) {
        whenever(uri.getQueryParameter(queryParam1.first)).thenReturn(queryParam1.second)
    }
    if (queryParam2 != null) {
        whenever(uri.getQueryParameter(queryParam2.first)).thenReturn(queryParam2.second)
    }
    return uri
}
