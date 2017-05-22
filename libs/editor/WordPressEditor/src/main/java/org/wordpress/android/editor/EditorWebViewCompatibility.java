package org.wordpress.android.editor;

import android.content.Context;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.WebView;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>Compatibility <code>EditorWebView</code> for pre-Chromium WebView (API<19). Provides a custom method for executing
 * JavaScript, {@link #loadJavaScript(String)}, instead of {@link WebView#loadUrl(String)}. This is needed because
 * <code>WebView#loadUrl(String)</code> on API<19 eventually calls <code>WebViewClassic#hideSoftKeyboard()</code>,
 * hiding the keyboard whenever JavaScript is executed.</p>
 * <p/>
 * <p>This class uses reflection to access the normally inaccessible <code>WebViewCore#sendMessage(Message)</code>
 * and use it to execute JavaScript, sidestepping <code>WebView#loadUrl(String)</code> and the keyboard issue.</p>
 */
@SuppressWarnings("TryWithIdenticalCatches")
public class EditorWebViewCompatibility extends EditorWebViewAbstract {
    public interface ReflectionFailureListener {
        void onReflectionFailure(ReflectionException e);
    }

    public class ReflectionException extends Exception {
        public ReflectionException(Throwable cause) {
            super(cause);
        }
    }

    private static final int EXECUTE_JS = 194; // WebViewCore internal JS message code

    private Object mWebViewCore;
    private Method mSendMessageMethod;

    // Dirty static listener, but it's impossible to set the listener during the construction if we want to keep
    // the xml layout
    private static ReflectionFailureListener mReflectionFailureListener;
    private boolean mReflectionSucceed = true;

    public static void setReflectionFailureListener(ReflectionFailureListener reflectionFailureListener) {
        mReflectionFailureListener = reflectionFailureListener;
    }

    public EditorWebViewCompatibility(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            this.initReflection();
        } catch (ReflectionException e) {
            AppLog.e(T.EDITOR, e);
            handleReflectionFailure(e);
        }
    }

    private void initReflection() throws ReflectionException {
        if (!mReflectionSucceed) {
            // Reflection failed once already, it won't succeed on a second try
            return;
        }
        Object webViewProvider;
        try {
            // On API >= 16, the WebViewCore instance is not defined inside WebView itself but inside a
            // WebViewClassic (implementation of WebViewProvider), referenced from the WebView as mProvider

            // Access WebViewClassic object
            Field webViewProviderField = WebView.class.getDeclaredField("mProvider");
            webViewProviderField.setAccessible(true);
            webViewProvider = webViewProviderField.get(this);

            // Access WebViewCore object
            Field webViewCoreField = webViewProvider.getClass().getDeclaredField("mWebViewCore");
            webViewCoreField.setAccessible(true);
            mWebViewCore = webViewCoreField.get(webViewProvider);

            // Access WebViewCore#sendMessage(Message) method
            if (mWebViewCore != null) {
                mSendMessageMethod = mWebViewCore.getClass().getDeclaredMethod("sendMessage", Message.class);
                mSendMessageMethod.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            throw new ReflectionException(e);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    private void loadJavaScript(final String javaScript) throws ReflectionException {
        if (mSendMessageMethod == null) {
            initReflection();
        } else {
            Message jsMessage = Message.obtain(null, EXECUTE_JS, javaScript);
            try {
                mSendMessageMethod.invoke(mWebViewCore, jsMessage);
            } catch (InvocationTargetException e) {
                throw new ReflectionException(e);
            } catch (IllegalAccessException e) {
                throw new ReflectionException(e);
            }
        }
    }

    public void execJavaScriptFromString(String javaScript) {
        try {
            loadJavaScript(javaScript);
        } catch (ReflectionException e) {
            AppLog.e(T.EDITOR, e);
            handleReflectionFailure(e);
        }
    }

    private void handleReflectionFailure(ReflectionException e) {
        if (mReflectionFailureListener != null) {
            mReflectionFailureListener.onReflectionFailure(e);
        }
        mReflectionSucceed = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mReflectionFailureListener = null;
    }
}
