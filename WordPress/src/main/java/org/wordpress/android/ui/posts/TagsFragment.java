package org.wordpress.android.ui.posts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.util.ActivityUtils;

import javax.inject.Inject;

import static org.wordpress.android.ui.posts.PostSettingsTagsActivity.KEY_TAGS;

public abstract class TagsFragment extends Fragment implements TextWatcher, View.OnKeyListener, TagSelectedListener {
    private SiteModel mSite;

    private EditText mTagsEditText;
    private TagsRecyclerViewAdapter mAdapter;

    @Inject Dispatcher mDispatcher;
    @Inject TaxonomyStore mTaxonomyStore;

    private String mTags;

    TagsSelectedListener mTagsSelectedListener;

    public TagsFragment() {
    }

    protected abstract @LayoutRes int getContentLayout();

    protected abstract String getTagsFromEditPostRepositoryOrArguments();

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            mTags = getArguments().getString(KEY_TAGS);

            if (mSite == null) {
                throw new IllegalStateException("Required argument mSite is missing.");
            }
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        mTagsSelectedListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(getContentLayout(), container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.tags_suggestion_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        mAdapter = new TagsRecyclerViewAdapter(requireActivity(), this);
        mAdapter.setAllTags(mTaxonomyStore.getTagsForSite(mSite));
        recyclerView.setAdapter(mAdapter);

        mTagsEditText = (EditText) view.findViewById(R.id.tags_edit_text);
        mTagsEditText.setOnKeyListener(this);
        mTagsEditText.requestFocus();
        ActivityUtils.showKeyboard(mTagsEditText);
        mTagsEditText.post(() -> mTagsEditText.addTextChangedListener(TagsFragment.this));

        loadTags();

        if (!TextUtils.isEmpty(mTags)) {
            // add a , at the end so the user can start typing a new tag
            mTags += ",";
            mTags = StringEscapeUtils.unescapeHtml4(mTags);
            mTagsEditText.setText(mTags);
            mTagsEditText.setSelection(mTagsEditText.length());
        }
        filterListForCurrentText();
    }

    private void loadTags() {
        mTags = getTagsFromEditPostRepositoryOrArguments();
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
            && (keyCode == KeyEvent.KEYCODE_ENTER)) {
            // Since we don't allow new lines, we should add comma on "enter" to separate the tags
            String currentText = mTagsEditText.getText().toString();
            if (!currentText.isEmpty() && !currentText.endsWith(",")) {
                mTagsEditText.setText(currentText.concat(","));
                mTagsEditText.setSelection(mTagsEditText.length());
            }
            return true;
        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // No-op
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        filterListForCurrentText();
        mTagsSelectedListener.onTagsSelected(charSequence.toString());
    }

    @Override
    public void afterTextChanged(Editable editable) {
        // No-op
    }

    // Find the text after the last occurrence of "," and filter with it
    private void filterListForCurrentText() {
        String text = mTagsEditText.getText().toString();
        int endIndex = text.lastIndexOf(",");
        if (endIndex == -1) {
            mAdapter.filter(text);
        } else {
            String textToFilter = text.substring(endIndex + 1).trim();
            mAdapter.filter(textToFilter);
        }
    }

    public void onTagSelected(@NonNull String selectedTag) {
        String text = mTagsEditText.getText().toString();
        String updatedText;
        int endIndex = text.lastIndexOf(",");
        if (endIndex == -1) {
            // no "," found, replace the current text with the selectedTag
            updatedText = selectedTag;
        } else {
            // there are multiple tags already, only update the text after the last ","
            updatedText = text.substring(0, endIndex + 1) + selectedTag;
        }
        updatedText += ",";
        updatedText = StringEscapeUtils.unescapeHtml4(updatedText);
        mTagsEditText.setText(updatedText);
        mTagsEditText.setSelection(mTagsEditText.length());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        switch (event.causeOfChange) {
            case FETCH_TAGS:
                mAdapter.setAllTags(mTaxonomyStore.getTagsForSite(mSite));
                filterListForCurrentText();
                break;
        }
    }

    void closeKeyboard() {
        ActivityUtils.hideKeyboardForced(mTagsEditText);
    }
}

