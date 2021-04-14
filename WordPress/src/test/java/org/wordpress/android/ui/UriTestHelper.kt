package org.wordpress.android.ui

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.wordpress.android.util.UriWrapper

fun buildUri(host: String? = null, path1: String, path2: String? = null, path3: String? = null): UriWrapper {
    val uri = mock<UriWrapper>()
    if (host != null) {
        whenever(uri.host).thenReturn(host)
    }
    whenever(uri.pathSegments).thenReturn(listOfNotNull(path1, path2, path3))
    return uri
}
