package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.suggestion.adapters.TagSuggestionAdapter;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostSettingsTagsActivity extends AppCompatActivity {
    public static final String KEY_TAG_LIST = "KEY_TAG_LIST";
    private SiteModel mSite;
    private List<String> mTagList;

    private SuggestionAutoCompleteText mTagsEditText;
    private TagsRecyclerViewAdapter mAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] tagsArray;
        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            tagsArray = getIntent().getStringArrayExtra(KEY_TAG_LIST);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            tagsArray = savedInstanceState.getStringArray(KEY_TAG_LIST);
        }
        mTagList = tagsArray != null ? Arrays.asList(tagsArray) : new ArrayList<String>();
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.post_settings_tags_fragment);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mTagsEditText = (SuggestionAutoCompleteText) findViewById(R.id.tags_edit_text);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.tags_suggestion_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new TagsRecyclerViewAdapter(this);
        recyclerView.setAdapter(mAdapter);
        if (mTagsEditText != null) {
            mTagsEditText.setTokenizer(new SuggestionAutoCompleteText.CommaTokenizer());

            setupSuggestionServiceAndAdapter();
        }

        String tags = TextUtils.join(",", mTagList);
        if (!tags.equals("") && mTagsEditText != null) {
            mTagsEditText.setText(tags);
        }
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
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
        Bundle bundle = new Bundle();
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setupSuggestionServiceAndAdapter() {
        long remoteSiteId = mSite.getSiteId();
        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(this, remoteSiteId);
        TagSuggestionAdapter tagSuggestionAdapter = SuggestionUtils.setupTagSuggestions(mSite, this,
                mSuggestionServiceConnectionManager);
        if (tagSuggestionAdapter != null) {
            mTagsEditText.setAdapter(tagSuggestionAdapter);
        }
    }

    private class TagsRecyclerViewAdapter extends RecyclerView.Adapter<TagsRecyclerViewAdapter.TagViewHolder> {
        private List<String> mFilteredTags;
        private Context mContext;

        TagsRecyclerViewAdapter(Context context) {
            mContext = context;
            mFilteredTags = new ArrayList<>(mTagList);
        }

        @Override
        public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tags_list_row, parent, false);
            return new TagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final TagViewHolder holder, int position) {
            holder.nameTextView.setText(mFilteredTags.get(position));
        }

        @Override
        public int getItemCount() {
            return mFilteredTags.size();
        }

        public void filter(final String text) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mFilteredTags.clear();
                    if (TextUtils.isEmpty(text)) {
                        mFilteredTags.addAll(mTagList);
                    } else {
                        for (String tag : mTagList) {
                            if (tag.toLowerCase().contains(text.toLowerCase())) {
                                mFilteredTags.add(tag);
                            }
                        }
                    }

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            }).start();

        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameTextView;

            TagViewHolder(View view) {
                super(view);
                nameTextView = (TextView) view.findViewById(R.id.tag_name);
            }
        }
    }
}
