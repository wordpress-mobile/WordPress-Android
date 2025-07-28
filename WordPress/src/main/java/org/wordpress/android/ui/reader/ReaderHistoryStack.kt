package org.wordpress.android.ui.reader

import android.os.Bundle
import java.util.Stack

/*
 * when previewing posts with a specific tag, a history of previewed tags is retained so
 * the user can navigate back through them - this is faster and requires less memory
 * than creating a new fragment for each previewed tag
 */
class ReaderHistoryStack(private val keyName: String) : Stack<String?>() {
    fun restoreInstance(bundle: Bundle) {
        clear()
        if (bundle.containsKey(keyName)) {
            val history = bundle.getStringArrayList(keyName)
            if (history != null) {
                this.addAll(history)
            }
        }
    }

    fun saveInstance(bundle: Bundle) {
        if (!isEmpty()) {
            val history = ArrayList<String>()
            history.addAll(this.filterNotNull())
            bundle.putStringArrayList(keyName, history)
        }
    }
}
