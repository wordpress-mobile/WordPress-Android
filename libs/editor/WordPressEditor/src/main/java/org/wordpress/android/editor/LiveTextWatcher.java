package org.wordpress.android.editor;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.Editable;
import android.text.TextWatcher;

public class LiveTextWatcher implements TextWatcher {
    private MutableLiveData<Editable> mAfterTextChanged = new MutableLiveData<>();
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override public void afterTextChanged(Editable s) {
        mAfterTextChanged.postValue(s);
    }

    public LiveData<Editable> getAfterTextChanged() {
        return mAfterTextChanged;
    }
}
