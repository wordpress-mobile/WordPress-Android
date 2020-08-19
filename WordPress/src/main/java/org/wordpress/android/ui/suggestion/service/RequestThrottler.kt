package org.wordpress.android.ui.suggestion.service

class RequestThrottler<K>(
    private val msBeforeRefresh: Long = 60 * 1000L,
    private val currentTime: () -> Long = System::currentTimeMillis
) {
    private val timeSinceLastResponseMap = mutableMapOf<K, Long>()

    fun areResultsStale(key: K): Boolean {
        val timeOfLastResponse = timeSinceLastResponseMap[key]
        return timeOfLastResponse == null || currentTime() - timeOfLastResponse >= msBeforeRefresh
    }

    fun onResponseReceived(key: K) {
        timeSinceLastResponseMap[key] = currentTime()
    }
}
