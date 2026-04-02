package org.wordpress.android.ui.reader.repository

sealed class ReaderRepositoryEvent {
    object ReaderPostTableActionEnded : ReaderRepositoryEvent()
}
