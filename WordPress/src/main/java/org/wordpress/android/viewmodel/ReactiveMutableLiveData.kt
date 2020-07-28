package org.wordpress.android.viewmodel

import androidx.lifecycle.MutableLiveData

/** ReactiveMutableLiveData is a simple extension of MutableLiveData.
 * It's purpose is to allow users to monitor onActive and onInactive
 *  situations because those methods are unreachable using straight up LiveData
 */
class ReactiveMutableLiveData<T>(private val onReactiveListener: OnReactiveListener) :
        MutableLiveData<T>() {
    // Allow a way to hook up the external listeners
    constructor(onActive: () -> Unit = {}, onInactive: () -> Unit = {}) : this(
            setReactiveListener(onActive, onInactive)
    )

    override fun onActive() {
        onReactiveListener.onActive()
    }

    override fun onInactive() {
        onReactiveListener.onInactive()
    }

    companion object {
        /**
         * Creates a OnReactiveListener that can be passed into the constructor
         */
        fun setReactiveListener(onActive: () -> Unit, onInactive: () -> Unit): OnReactiveListener {
            return object : OnReactiveListener {
                override fun onActive() {
                    onActive.invoke()
                }

                override fun onInactive() {
                    onInactive.invoke()
                }
            }
        }
    }
}

interface OnReactiveListener {
    fun onActive()
    fun onInactive()
}
