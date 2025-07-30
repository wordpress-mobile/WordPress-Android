package org.wordpress.android.fluxc.network.rest.wpcom.media

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val progressListener: ProgressListener
) : RequestBody() {
    interface ProgressListener {
        fun onProgress(bytesWritten: Long, contentLength: Long)
    }

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val contentLength = contentLength()
        val progressSink = ProgressSink(sink, contentLength, progressListener)
        val bufferedSink = progressSink.buffer()

        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private class ProgressSink(
        delegate: Sink,
        private val contentLength: Long,
        private val listener: ProgressListener
    ) : ForwardingSink(delegate) {
        private var totalBytesWritten = 0L
        private var lastProgressTime = 0L
        
        // Throttle to 2 calls per second (500ms minimum between calls)
        private val progressThrottleMs = 500L

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            totalBytesWritten += byteCount
            
            val currentTime = System.currentTimeMillis()
            val isComplete = totalBytesWritten >= contentLength
            
            // Always send progress for completion, or if enough time has passed
            if (isComplete || currentTime - lastProgressTime >= progressThrottleMs) {
                listener.onProgress(totalBytesWritten, contentLength)
                lastProgressTime = currentTime
            }
        }
    }
}
