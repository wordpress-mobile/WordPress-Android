package org.wordpress.android.widgets;

import android.view.View;
import android.widget.EditText;

import org.wordpress.android.datasets.AutoSaveTextTable;

public class AutoSaveTextHelper {
    private String mUniqueId;

    public void setUniqueId(String uniqueId) {
        mUniqueId = uniqueId;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    private static String getViewPathId(View view) {
        StringBuilder sb = new StringBuilder();
        for (View currentView = view; currentView != null && currentView.getParent() != null
                && currentView.getParent() instanceof View; currentView = (View) currentView.getParent()) {
            sb.append(currentView.getId());
        }
        return sb.toString();
    }

    public void clearSavedText(View view) {
        clearSavedText(view, mUniqueId);
    }

    public void loadString(EditText editText) {
        String text = AutoSaveTextTable.get(getViewPathId(editText) + mUniqueId, "");
        if (!text.isEmpty()) {
            editText.setText(text);
            editText.setSelection(text.length());
        }
    }

    public void saveString(EditText editText) {
        if (editText.getText() == null) {
            return;
        }
        AutoSaveTextTable.put(getViewPathId(editText) + mUniqueId, editText.getText().toString());
    }

    public static void clearSavedText(View view, String uniqueId) {
        AutoSaveTextTable.remove(getViewPathId(view) + uniqueId);
    }
}
