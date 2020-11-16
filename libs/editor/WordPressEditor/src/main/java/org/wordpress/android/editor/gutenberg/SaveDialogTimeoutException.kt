package org.wordpress.android.editor.gutenberg

class SaveDialogTimeoutException(timeoutDuration: Int) : Exception(timeoutMessage(timeoutDuration)) {
    companion object {
        private fun timeoutMessage(timeoutDuration: Int): String =
                "The Gutenberg editor's save dialog timeout fired after $timeoutDuration " +
                        "and automatically dismissed the dialog to ensure the user was not " +
                        "left with a blocking dialog. This indicates either that the local save " +
                        "was slow or that we failed to properly dismiss the dialog."
    }
}
