package org.wordpress.android.viewmodel.quickstart

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.QuickStartTaskState
import javax.inject.Inject

class QuickStartViewModel @Inject constructor(private val quickStartStore: QuickStartStore) : ViewModel() {
    private val _quickStartTaskStateStates = MutableLiveData<List<QuickStartTaskState>>()
    val quickStartTaskStateStates: LiveData<List<QuickStartTaskState>> = _quickStartTaskStateStates

    private var isStarted = false
    var siteId: Long = 0

    fun start(siteId: Long) {
        if (isStarted) {
            return
        }

        this.siteId = siteId

        refreshTaskStatus()

        isStarted = true
    }

    private fun refreshTaskStatus() {
        val list = ArrayList<QuickStartTaskState>()
        QuickStartTask.values().forEach {
            list.add(QuickStartTaskState(it, quickStartStore.hasDoneTask(siteId, it)))
        }
        _quickStartTaskStateStates.postValue(list)
    }

    fun completeTask(task: QuickStartTask, isCompleted: Boolean) {
        quickStartStore.setDoneTask(siteId, task, isCompleted)
        refreshTaskStatus()
    }

    fun skipAllTasks() {
        QuickStartTask.values().forEach { quickStartStore.setDoneTask(siteId, it, true) }
        quickStartStore.setQuickStartCompleted(siteId, true)
        // skipping all tasks means no achievement notification, so we mark it as received
        quickStartStore.setQuickStartNotificationReceived(siteId, true)
        refreshTaskStatus()
    }
}
