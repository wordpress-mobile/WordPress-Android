package org.wordpress.android.util

import android.arch.lifecycle.MutableLiveData

class DistinctMutableLiveData<T>(private val defaultValue: T) : MutableLiveData<T>() {
    override fun postValue(value: T) {
        if (this.value != value) {
            super.postValue(value)
        }
    }

    override fun setValue(value: T) {
        if (this.value != value) {
            super.setValue(value)
        }
    }

    override fun getValue(): T {
        return super.getValue() ?: defaultValue
    }

    fun clear() {
        value = defaultValue
    }
}
