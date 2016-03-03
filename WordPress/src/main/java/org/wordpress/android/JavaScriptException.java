package org.wordpress.android;

public class JavaScriptException extends Throwable {
    String mFile;
    int mLine;

    public JavaScriptException(String file, int line, String message) {
        super(message);
        mFile = file;
        mLine = line;
        fillInStackTrace();
    }

    @Override
    public Throwable fillInStackTrace() {
        setStackTrace(new StackTraceElement[] {
                new StackTraceElement("JavaScriptException", "", mFile, mLine)
        });
        return this;
    }
}
