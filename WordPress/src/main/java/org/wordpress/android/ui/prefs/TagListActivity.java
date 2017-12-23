package org.wordpress.android.ui.prefs;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
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

public class TagListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;

    private static final String SAVED_QUERY = "SAVED_QUERY";

    private SiteModel mSite;
    private RecyclerView mRecycler;
    private TagListAdapter mAdapter;
    private String mQuery;

    private Menu mMenu;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

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
            mQuery = savedInstanceState.getString(SAVED_QUERY);
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
        outState.putString(SAVED_QUERY, mQuery);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_search) {
            mSearchMenuItem = item;
            mSearchMenuItem.expandActionView();

            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);

            if (!TextUtils.isEmpty(mQuery)) {
                onQueryTextSubmit(mQuery);
                mSearchView.setQuery(mQuery, true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            TagDetailFragment fragment = getDetailFragment();
            if (fragment != null) {
                fragment.saveChanges();
            }
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.tag_list, menu);

        mSearchMenuItem = menu.findItem(R.id.menu_search);

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        // open search bar if we were searching for something before
        if (!TextUtils.isEmpty(mQuery)) {
            String tempQuery = mQuery;
            mSearchMenuItem.expandActionView();
            onQueryTextSubmit(tempQuery);
            mSearchView.setQuery(mQuery, true);
        }

        return super.onCreateOptionsMenu(menu);
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

    private TagDetailFragment getDetailFragment() {
        return (TagDetailFragment) getFragmentManager().findFragmentByTag(TagDetailFragment.TAG);
    }

    private void showTagDetail(@NonNull TermModel term) {
        mSearchMenuItem.collapseActionView();
        TagDetailFragment fragment = getDetailFragment();
        if (fragment == null) {
            fragment = TagDetailFragment.newInstance(mSite, term.getRemoteTermId());
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment, TagDetailFragment.TAG)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mAdapter.filter(query);
        mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mAdapter.filter(query);
        return true;
    }

    private class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.TagViewHolder> {
        private final List<TermModel> mAllTags = new ArrayList<>();
        private final List<TermModel> mFilteredTags = new ArrayList<>();

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
            TermModel term = mFilteredTags.get(position);
            holder.txtTag.setText(StringEscapeUtils.unescapeHtml4(term.getName()));
            if (term.getPostCount() > 0) {
                holder.txtCount.setVisibility(View.VISIBLE);
                holder.txtCount.setText(String.valueOf(term.getPostCount()));
            } else {
                holder.txtCount.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mFilteredTags.size();
        }

        public void filter(final String text) {
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
            private final TextView txtCount;

            TagViewHolder(View view) {
                super(view);
                txtTag = view.findViewById(R.id.text_tag);
                txtCount = view.findViewById(R.id.text_count);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        showTagDetail(mFilteredTags.get(position));
                    }
                });
            }
        }
    }
}
