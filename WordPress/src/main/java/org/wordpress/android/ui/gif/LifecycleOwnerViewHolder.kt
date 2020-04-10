package org.wordpress.android.ui.gif

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * A [ViewHolder] that is also a [LifecycleOwner]
 *
 * This allows subclasses to observe [LiveData] objects:
 *
 * ```
 * mediaViewModel?.isSelected?.observe(this, Observer {
 *     // do something
 * })
 * ```
 *
 * The observers will be automatically removed on 2 occasions:
 *
 * - When the [ViewHolder.itemView]'s parent ([RecyclerView]) is removed from the window
 * - When the [ViewHolder] is bound with a different item. See [bind].
 *
 * For this to work, subclasses should make use of [bind] when binding items (i.e. ViewModels). For example, in a
 * [ListAdapter]:
 *
 * ```
 * override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
 *     holder.bind(getItem(position))
 * }
 * ```
 * The subclass should make sure to call `super.bind()`:
 *
 * ```
 * class MyViewHolder(itemView: View): LifecycleOwnerViewHolder<MyItem> {
 *     override fun bind(item: MyItem?) {
 *         super.bind(item)
 *
 *         // observe LiveData here
 *     }
 * }
 * ```
 */
abstract class LifecycleOwnerViewHolder<T>(itemView: View) : ViewHolder(itemView), LifecycleOwner {
    /**
     * Investigated the leak warning, the [LifecycleRegistry] constructor does not seem to be doing anything with
     * the current object apart from setting a weak reference to it.
     */
    @Suppress("LeakingThis")
    private val lifecycleRegistry = LifecycleRegistry(this)

    /**
     * Tracks if we have already added [parentOnAttachStateChangeListener] to the parent.
     *
     * This is so we don't add it to the parent multiple times when the [itemView] `onViewAttachedToWindow` is
     * called again.
     */
    private var hasAddedParentOnAttachStateChangeListener = false

    /**
     * Set the lifecycle state to [State.DESTROYED] when the parent (i.e. [RecyclerView]) is removed from its window.
     *
     * We cannot rely on the [itemView]'s `onViewDetachedFromWindow` event to set the state to [State.DESTROYED]
     * because the [RecyclerView] will sometimes call these methods in sequence:
     *
     * 1. itemView `bind` -- state is STARTED (observers are created)
     * 2. itemView `onViewDetachedFromWindow` -- state is DESTROYED (observers are removed)
     * 3. itemView `onViewAttachedToWindow` -- state is STARTED
     *
     * The 2nd step causes all observers to be removed and even if the view is shown (attached) again. This leaves us
     * with a [ViewHolder] that does not react to anything anymore.
     *
     * Only removing when the parent [RecyclerView] is probably not ideal but it solves the problem of observers
     * leaking even if the [RecyclerView] and [ViewHolder] have been removed from the window.
     */
    private val parentOnAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewDetachedFromWindow(view: View) {
            assert(view is RecyclerView)

            // Setting the state to DESTROYED removes the observers initialized in bind()
            lifecycleRegistry.markState(State.DESTROYED)
        }

        override fun onViewAttachedToWindow(view: View) {
            // noop
        }
    }

    init {
        lifecycleRegistry.markState(State.INITIALIZED)

        itemView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                if (!hasAddedParentOnAttachStateChangeListener) {
                    assert(view.parent is RecyclerView)

                    (view.parent as ViewGroup).addOnAttachStateChangeListener(parentOnAttachStateChangeListener)
                    hasAddedParentOnAttachStateChangeListener = true
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                // noop
            }
        })
    }

    /**
     * Bind [item] to this ViewHolder
     *
     * This changes the lifecycle state to [State.DESTROYED] to remove previously created observers from possibly a
     * different [item] object. The state is then moved to [State.CREATED] so we can add observers and they will
     * be active.
     *
     * We cannot rely on the [itemView]'s `onViewDetachedFromWindow` event to change the state to [State.DESTROYED]
     * because the [RecyclerView] will sometimes call [bind] without ever removing the [itemView] from the window.
     * That leaves us with observers from a previous [item] not getting removed.
     */
    open fun bind(item: T?) {
        lifecycleRegistry.markState(State.DESTROYED)
        lifecycleRegistry.markState(State.STARTED)
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}
