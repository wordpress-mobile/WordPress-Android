package org.wordpress.android.util.image

import android.graphics.drawable.Drawable
import android.view.animation.AnimationUtils
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.transition.NoTransition
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.TransitionFactory
import org.wordpress.android.R
import org.wordpress.android.util.AppLog
import java.lang.RuntimeException

object GlidePopTransitionOptions : TransitionOptions<GlidePopTransitionOptions, Drawable>() {
    fun pop(): GlidePopTransitionOptions {
        return transition(GlidePopTransitionFactory())
    }
}

class GlidePopTransition : Transition<Drawable> {
    @Suppress("TooGenericExceptionCaught")
    override fun transition(current: Drawable?, adapter: Transition.ViewAdapter?): Boolean {
        adapter?.view?.context?.let {
            adapter.setDrawable(current)
            try {
                val pop = AnimationUtils.loadAnimation(it, R.anim.pop)
                adapter.view.startAnimation(pop)
            } catch (e: RuntimeException) {
                AppLog.e(AppLog.T.UTILS, "Error animating drawable: $e")
            }
        }
        return true
    }
}

class GlidePopTransitionFactory : TransitionFactory<Drawable> {
    private val transition: GlidePopTransition by lazy { GlidePopTransition() }
    override fun build(dataSource: DataSource?, isFirstResource: Boolean): Transition<Drawable> {
        return if (dataSource == DataSource.MEMORY_CACHE) NoTransition.get<Drawable>() else transition
    }
}
