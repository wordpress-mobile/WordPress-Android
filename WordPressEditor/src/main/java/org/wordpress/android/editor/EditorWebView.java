package org.wordpress.android.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class EditorWebView extends WebView {

    public EditorWebView(Context context) {
        super(context);
    }

    public EditorWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditorWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    public void execJavaScriptFromString(String javaScript) {
        this.loadUrl("javascript:" + javaScript);
    }

}