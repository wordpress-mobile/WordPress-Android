package org.wordpress.android.viewmodel.activitylog

import android.arch.paging.PositionalDataSource

abstract class BaseDataSource<T> : PositionalDataSource<T>() {
    protected abstract fun countItems(): Int
    protected abstract fun loadRangeAtPosition(position: Int, size: Int): List<T>?

    override fun loadInitial(params: PositionalDataSource.LoadInitialParams,
                             callback: PositionalDataSource.LoadInitialCallback<T>) {
        val total = countItems()

        if (total == 0) {
            callback.onResult(emptyList<T>(), 0, 0)
        } else {
            val position = PositionalDataSource.computeInitialLoadPosition(params, total)
            val size = PositionalDataSource.computeInitialLoadSize(params, position, total)
            val list = loadRangeAtPosition(position, size)

            if (list != null && list.size == size) {
                callback.onResult(list, position, total)
            } else {
                invalidate()
            }
        }
    }

    override fun loadRange(params: PositionalDataSource.LoadRangeParams,
                           callback: PositionalDataSource.LoadRangeCallback<T>) {
        val list = loadRangeAtPosition(params.startPosition, params.loadSize)

        if (list != null) {
            callback.onResult(list)
        } else {
            invalidate()
        }
    }
}
