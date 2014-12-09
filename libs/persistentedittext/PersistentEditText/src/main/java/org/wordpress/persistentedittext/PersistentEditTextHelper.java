package org.wordpress.persistentedittext;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

public class PersistentEditTextHelper {
    private String mUniqueId;
    private PersistentEditTextDatabase mPersistentEditTextDatabase;

    public PersistentEditTextHelper(Context context) {
        mPersistentEditTextDatabase = new PersistentEditTextDatabase(context);
    }

    /**
     * Set a unique id. This is only needed to identify a text edit from another if they share the same layout
     * path and id.
     *
     * For instance if you want to use the same fragment with one text field to display different content and want
     * the saved text in the EditField to be differentiated, you'll have to set that id. Another example: if you have
     * a listview with an EditText in each rows, you might want to set a different id for each row.
     */
    public void setUniqueId(String uniqueId) {
        mUniqueId = uniqueId;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    public void clearSavedText(View view) {
        clearSavedText(view, mUniqueId);
    }

    public void loadString(EditText editText) {
        String text = mPersistentEditTextDatabase.get(getViewPathId(editText) + mUniqueId, "");
        if (!text.isEmpty()) {
            editText.setText(text);
            editText.setSelection(text.length());
        }
    }

    public void saveString(EditText editText) {
        if (editText.getText() == null) {
            return;
        }
        mPersistentEditTextDatabase.put(getViewPathId(editText) + mUniqueId, editText.getText().toString());
    }

    public void clearSavedText(View view, String uniqueId) {
        mPersistentEditTextDatabase.remove(getViewPathId(view) + uniqueId);
    }

    protected static String getViewPathId(View view) {
        StringBuilder sb = new StringBuilder();
        for (View currentView = view; currentView != null && currentView.getParent() != null
                && currentView.getParent() instanceof View; currentView = (View) currentView.getParent()) {
            sb.append(currentView.getId());
        }
        return sb.toString();
    }
}
