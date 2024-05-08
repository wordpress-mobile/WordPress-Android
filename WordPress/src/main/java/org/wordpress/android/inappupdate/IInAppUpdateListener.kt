package org.wordpress.android.inappupdate

interface IInAppUpdateListener {
    fun onAppUpdateStarted(type: Int)
    fun onAppUpdateDownloaded()
    fun onAppUpdateInstalled()
    fun onAppUpdateFailed()
    fun onAppUpdateCancelled()
    fun onAppUpdatePending()
}
