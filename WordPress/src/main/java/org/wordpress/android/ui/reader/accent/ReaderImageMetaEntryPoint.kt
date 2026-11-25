package org.wordpress.android.ui.reader.accent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReaderImageMetaEntryPoint {
    fun imageMetaStore(): ReaderImageMetaStore
}

/**
 * The sampled accent and aspect ratio of the image at [url], updating once it's been sampled. Null in previews,
 * which have no Hilt graph.
 */
@Composable
fun readerImageMeta(url: String?): ReaderImageMeta? {
    if (url == null || LocalInspectionMode.current) return null
    val context = LocalContext.current.applicationContext
    val store = remember(context) {
        EntryPointAccessors.fromApplication(context, ReaderImageMetaEntryPoint::class.java).imageMetaStore()
    }
    val all = store.meta.collectAsState()
    // only this image's entry, so another image being sampled doesn't recompose the caller
    val meta by remember(url) { derivedStateOf { all.value[url] } }
    return meta
}
