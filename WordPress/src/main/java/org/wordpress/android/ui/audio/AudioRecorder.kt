package org.wordpress.android.ui.audio

interface AudioRecorder {
    fun start(audioOutputRequestType: AudioOutputRequestType)
    fun stop()
}

// todo: annmarie - maybe this can be expanded to include an output package
// that includes a file name and type
enum class AudioOutputRequestType {
    FILE,
    MEMORY
}

