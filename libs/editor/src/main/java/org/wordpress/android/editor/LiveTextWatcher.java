package org.wordpress.android.editor;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

    public void postTextChanged() {
        mAfterTextChanged.postValue(null);
    }
}
