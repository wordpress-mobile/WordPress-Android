package org.wordpress.android.editor;

import android.content.Context;
import android.util.AttributeSet;

public class EditorWebView extends EditorWebViewAbstract {

    public EditorWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void execJavaScriptFromString(String javaScript) {
        this.loadUrl("javascript:" + javaScript);
    }

}