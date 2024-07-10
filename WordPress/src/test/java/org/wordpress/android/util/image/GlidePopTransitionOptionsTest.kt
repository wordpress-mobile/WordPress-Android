package org.wordpress.android.util.image

import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.transition.NoTransition
import com.bumptech.glide.request.transition.Transition
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class GlidePopTransitionOptionsTest : BaseUnitTest() {
    @Test
    fun testBuildWithCache() {
        val glidePopTransitionFactory = GlidePopTransitionFactory()

        val result = glidePopTransitionFactory.build(DataSource.MEMORY_CACHE, true)

        TestCase.assertTrue(result is NoTransition)
    }

    @Test
    fun testBuildWithNoCache() {
        val glidePopTransitionFactory = GlidePopTransitionFactory()

        val result = glidePopTransitionFactory.build(DataSource.REMOTE, true)

        TestCase.assertTrue(result is GlidePopTransition)
    }

    @Test
    fun testTransition() {
        val glidePopTransition = GlidePopTransition()
        val drawable: Drawable = mock()
        val viewAdapter: Transition.ViewAdapter = mock()
        val view: View = mock()
        whenever(viewAdapter.view).thenReturn(view)
        whenever(view.context).thenReturn(mock())

        val result = glidePopTransition.transition(drawable, viewAdapter)

        TestCase.assertTrue(result)
        verify(viewAdapter).setDrawable(drawable)
    }
}
