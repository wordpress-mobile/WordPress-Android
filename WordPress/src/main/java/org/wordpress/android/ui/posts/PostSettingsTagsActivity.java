package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class PostSettingsTagsActivity extends LocaleAwareActivity implements TextWatcher, View.OnKeyListener {
    public static final String KEY_TAGS = "KEY_TAGS";
    public static final String KEY_SELECTED_TAGS = "KEY_SELECTED_TAGS";
    private SiteModel mSite;

    private EditText mTagsEditText;
    private TagsRecyclerViewAdapter mAdapter;

    @Inject Dispatcher mDispatcher;
    @Inject TaxonomyStore mTaxonomyStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplicationContext()).component().inject(this);

        String tags = null;
        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            tags = getIntent().getStringExtra(KEY_TAGS);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.post_settings_tags_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tags_suggestion_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new TagsRecyclerViewAdapter(this);
        mAdapter.setAllTags(mTaxonomyStore.getTagsForSite(mSite));
        recyclerView.setAdapter(mAdapter);

        mTagsEditText = (EditText) findViewById(R.id.tags_edit_text);
        mTagsEditText.setOnKeyListener(this);
        mTagsEditText.addTextChangedListener(this);
        if (!TextUtils.isEmpty(tags)) {
            // add a , at the end so the user can start typing a new tag
            tags += ",";
            tags = StringEscapeUtils.unescapeHtml4(tags);
            mTagsEditText.setText(tags);
            mTagsEditText.setSelection(mTagsEditText.length());
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

    private void saveAndFinish() {
        ActivityUtils.hideKeyboardForced(mTagsEditText);

        Bundle bundle = new Bundle();
        bundle.putString(KEY_SELECTED_TAGS, mTagsEditText.getText().toString());
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
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

    private void onTagSelected(@NonNull String selectedTag) {
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

    private class TagsRecyclerViewAdapter extends RecyclerView.Adapter<TagsRecyclerViewAdapter.TagViewHolder> {
        private List<TermModel> mAllTags;
        private List<TermModel> mFilteredTags;
        private Context mContext;

        TagsRecyclerViewAdapter(Context context) {
            mContext = context;
            mFilteredTags = new ArrayList<>();
        }

        @Override
        public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tags_list_row, parent, false);
            return new TagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final TagViewHolder holder, int position) {
            // Guard against mFilteredTags getting altered in another thread
            if (mFilteredTags.size() <= position) {
                return;
            }
            String tag = StringEscapeUtils.unescapeHtml4(mFilteredTags.get(position).getName());
            holder.mNameTextView.setText(tag);
        }

        @Override
        public int getItemCount() {
            return mFilteredTags.size();
        }

        void setAllTags(List<TermModel> allTags) {
            mAllTags = allTags;
        }

        public void filter(final String text) {
            final List<TermModel> allTags = mAllTags;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<TermModel> filteredTags = new ArrayList<>();
                    if (TextUtils.isEmpty(text)) {
                        filteredTags.addAll(allTags);
                    } else {
                        for (TermModel tag : allTags) {
                            if (tag.getName().toLowerCase(Locale.getDefault())
                                   .contains(text.toLowerCase(Locale.getDefault()))) {
                                filteredTags.add(tag);
                            }
                        }
                    }

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFilteredTags = filteredTags;
                            notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            private final TextView mNameTextView;

            TagViewHolder(View view) {
                super(view);
                mNameTextView = (TextView) view.findViewById(R.id.tag_name);
                RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.tags_list_row_container);
                layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onTagSelected(mNameTextView.getText().toString());
                    }
                });
            }
        }
    }
}
