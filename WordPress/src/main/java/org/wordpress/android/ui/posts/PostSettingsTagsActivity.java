package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;

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
}
