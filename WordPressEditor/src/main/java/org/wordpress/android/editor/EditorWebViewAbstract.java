package org.wordpress.android.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public abstract class EditorWebViewAbstract extends WebView {
    public abstract void execJavaScriptFromString(String javaScript);

    public EditorWebViewAbstract(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }
}
