package org.wordpress.android.editor;

import androidx.core.util.Consumer;

public interface ExceptionLogger {
    Consumer<Exception> getExceptionLogger();
}
