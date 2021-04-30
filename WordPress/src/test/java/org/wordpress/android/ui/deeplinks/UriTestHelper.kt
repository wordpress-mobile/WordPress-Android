package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.wordpress.android.util.UriWrapper

fun buildUri(host: String? = null, path1: String? = null, path2: String? = null, path3: String? = null): UriWrapper {
    val uri = mock<UriWrapper>()
    if (host != null) {
        whenever(uri.host).thenReturn(host)
    }
    if (path1 != null || path2 != null || path3 != null) {
        whenever(uri.pathSegments).thenReturn(listOfNotNull(path1, path2, path3))
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
