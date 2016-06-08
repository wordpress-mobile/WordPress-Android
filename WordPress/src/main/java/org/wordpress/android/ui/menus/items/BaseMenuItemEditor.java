package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewStub;
import android.widget.LinearLayout;

import org.wordpress.android.models.MenuItemModel;

import java.util.HashMap;
import java.util.Map;

/**
 */
public abstract class BaseMenuItemEditor extends LinearLayout
        implements ViewStub.OnInflateListener {
    public abstract int getIconDrawable();
    protected abstract int getLayoutRes();
    public abstract MenuItemModel getMenuItem();
    public abstract void onSave();
    public abstract void onDelete();

    private Map<String, Object> mData = new HashMap<>();

    public BaseMenuItemEditor(Context context) {
        super(context);
        init();
    }

    @Override
    public void onInflate(ViewStub stub, View inflated) {
    }

    public void setData(@NonNull Map<String, Object> data) {
        mData = data;
    }

    public void setData(@NonNull String key, Object value) {
        mData.put(key, value);
    }

    public Object getData(@NonNull String key) {
        return mData.get(key);
    }

    private void init() {
        ViewStub stub = new ViewStub(getContext(), getLayoutRes());
        stub.setOnInflateListener(this);
        addView(stub);
    }
}
