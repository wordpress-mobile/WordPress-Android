package org.wordpress.android.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.editor.EditorFragment;
import org.wordpress.android.editor.EditorWebViewAbstract;
import org.wordpress.android.editor.R;

import java.util.Map;

public class EditorFragmentForTests extends EditorFragment {
    protected EditorWebViewAbstract mWebView;

    protected boolean mInitCalled = false;
    protected boolean mDomLoaded = false;
    protected boolean mOnSelectionStyleChangedCalled = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mWebView = (EditorWebViewAbstract) view.findViewById(R.id.webview);
        return view;
    }

    @Override
    protected void initJsEditor() {
        super.initJsEditor();
        mInitCalled = true;
    }

    @Override
    public void onDomLoaded() {
        super.onDomLoaded();
        mDomLoaded = true;
    }

    @Override
    public void onSelectionStyleChanged(final Map<String, Boolean> changeMap) {
        super.onSelectionStyleChanged(changeMap);
        mOnSelectionStyleChangedCalled = true;
    }
}
