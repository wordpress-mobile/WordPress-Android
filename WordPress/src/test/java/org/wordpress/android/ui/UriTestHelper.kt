package org.wordpress.android.ui

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
