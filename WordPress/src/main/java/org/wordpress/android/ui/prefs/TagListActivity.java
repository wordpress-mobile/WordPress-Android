package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.action.TaxonomyAction.FETCH_TAGS;

public class TagListActivity extends AppCompatActivity {

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;

    private SiteModel mSite;
    private RecyclerView mRecycler;
    private TagListAdapter mAdapter;

    public static void showTagList(@NonNull Context context, @NonNull SiteModel site) {
        Intent intent = new Intent(context, TagListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.tag_list_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        mRecycler = findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        loadTags();

        if (savedInstanceState == null) {
            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(mSite));
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(TaxonomyStore.OnTaxonomyChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.SETTINGS, event.error.message);
        } else if (event.causeOfChange == FETCH_TAGS) {
            loadTags();
        }
    }

    private void loadTags() {
        mAdapter = new TagListAdapter(mTaxonomyStore.getTagsForSite(mSite));
        mRecycler.setAdapter(mAdapter);
    }

    private void doTagSelected(@NonNull String selectedTag) {
        // TODO: show detail view
    }

    private class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.TagViewHolder> {
        private final List<TermModel> mAllTags = new ArrayList<>();
        private final List<TermModel> mFilteredTags = new ArrayList<>();
        private String mCurFilter;

        TagListAdapter(@NonNull List<TermModel> allTags) {
            mAllTags.addAll(allTags);
            mFilteredTags.addAll(allTags);
        }

        @Override
        public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_list_row, parent, false);
            return new TagListAdapter.TagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final TagListAdapter.TagViewHolder holder, int position) {
            String tag = StringEscapeUtils.unescapeHtml4(mFilteredTags.get(position).getName());
            holder.txtTag.setText(tag);
        }

        @Override
        public int getItemCount() {
            return mFilteredTags.size();
        }

        public void filter(final String text) {
            mCurFilter = text;
            mFilteredTags.clear();
            if (TextUtils.isEmpty(text)) {
                mFilteredTags.addAll(mAllTags);
            } else {
                for (TermModel tag : mAllTags) {
                    if (tag.getName().toLowerCase().contains(text.toLowerCase())) {
                        mFilteredTags.add(tag);
                    }
                }
            }
            notifyDataSetChanged();
        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtTag;

            TagViewHolder(View view) {
                super(view);
                txtTag = view.findViewById(R.id.text_tag);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doTagSelected(txtTag.getText().toString());
                    }
                });
            }
        }
    }
}
