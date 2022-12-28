@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import com.bumptech.glide.request.target.BaseTarget
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.util.image.ImageManager
import org.wordpress.aztec.Html.ImageGetter

class AztecImageLoaderTest {
    private lateinit var imageLoader: AztecImageLoader
    private lateinit var imageManager: ImageManager
    private lateinit var callback: ImageGetter.Callbacks
    private lateinit var bitmap: Bitmap

    private val url = "https://testingurl.com"

    @Before
    fun setUp() {
        callback = mock(ImageGetter.Callbacks::class.java)
        imageManager = mock(ImageManager::class.java)
        imageLoader = AztecImageLoader(mock(Context::class.java), imageManager, mock(Drawable::class.java))
        bitmap = mock(Bitmap::class.java)
    }

    @Test
    fun verifyClearTargetsClearsAllTargets() {
        // load 3 images
        imageLoader.loadImage(url, callback, -1)
        imageLoader.loadImage(url, callback, -1)
        imageLoader.loadImage(url, callback, -1)
        // clear targets
        imageLoader.clearTargets()
        // verify all three images were cleared
        verify(imageManager, times(3)).cancelRequest<Bitmap>(any(), any())
    }

    @Test
    fun onResourceReadyCallOnImageLoaded() {
        wheneverLoadAsBitmapInvokeOnResourceReady()
        // load an image
        imageLoader.loadImage(url, callback, -1)
        // verify onImageLoaded was invoked
        verify(callback, times(1)).onImageLoaded(any())
    }

    @Test
    fun onResourceFailedCallOnImageFailed() {
        wheneverLoadAsBitmapInvokeOnLoadFailed()
        // load an image
        imageLoader.loadImage(url, callback, -1)
        // verify onImageFailed was invoked
        verify(callback, times(1)).onImageFailed()
    }

    @Test
    fun onLoadStartedCallOnImageLoading() {
        wheneverLoadAsBitmapInvokeOnLoadStarted()
        // load an image
        imageLoader.loadImage(url, callback, -1)
        // verify onImageLoading was called
        verify(callback, times(1)).onImageLoading(any())
    }

    @Test
    fun onResourceReadySetBitmapDensityToDefault() {
        wheneverLoadAsBitmapInvokeOnResourceReady()
        // load an image
        imageLoader.loadImage(url, callback, -1)
        // verify bitmap density is set to DEFAULT
        verify(bitmap, times(1)).density = DisplayMetrics.DENSITY_DEFAULT
    }

    private fun wheneverLoadAsBitmapInvokeOnLoadFailed() {
        whenever(imageManager.loadAsBitmapIntoCustomTarget(any(), any(), any(), any()))
            .thenAnswer { invocation ->
                run {
                    @Suppress("DEPRECATION", "UNCHECKED_CAST")
                    (invocation.arguments[1] as BaseTarget<Bitmap>).onLoadFailed(mock(Drawable::class.java))
                }
            }
    }

    private fun wheneverLoadAsBitmapInvokeOnResourceReady() {
        whenever(imageManager.loadAsBitmapIntoCustomTarget(any(), any(), any(), any()))
            .thenAnswer { invocation ->
                run {
                    @Suppress("DEPRECATION", "UNCHECKED_CAST")
                    (invocation.arguments[1] as BaseTarget<Bitmap>).onResourceReady(bitmap, null)
                }
            }
    }

    private fun wheneverLoadAsBitmapInvokeOnLoadStarted() {
        whenever(imageManager.loadAsBitmapIntoCustomTarget(any(), any(), any(), any()))
            .thenAnswer { invocation ->
                run {
                    @Suppress("DEPRECATION", "UNCHECKED_CAST")
                    (invocation.arguments[1] as BaseTarget<Bitmap>).onLoadStarted(mock(Drawable::class.java))
                }
            }
    }
}
