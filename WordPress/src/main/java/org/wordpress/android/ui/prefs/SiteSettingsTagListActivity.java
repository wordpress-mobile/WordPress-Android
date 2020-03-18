package org.wordpress.android.ui.prefs;

import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ViewUtilsKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class SiteSettingsTagListActivity extends LocaleAwareActivity
        implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener,
        SiteSettingsTagDetailFragment.OnTagDetailListener {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;

    private static final String KEY_SAVED_QUERY = "SAVED_QUERY";
    private static final String KEY_PROGRESS_RES_ID = "PROGRESS_RESOURCE_ID";
    private static final String KEY_IS_SEARCHING = "IS_SEARCHING";

    private SiteModel mSite;
    private EmptyViewRecyclerView mRecycler;
    private View mFabView;
    private ActionableEmptyView mActionableEmptyView;

    private TagListAdapter mAdapter;
    private String mSavedQuery;
    private int mLastProgressResId;

    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;
    private ProgressDialog mProgressDialog;

    private boolean mIsSearching;

    public static void showTagList(@NonNull Context context, @NonNull SiteModel site) {
        Intent intent = new Intent(context, SiteSettingsTagListActivity.class);
        intent.putExtra(WordPress.SITE, site);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.site_settings_tag_list_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mSavedQuery = savedInstanceState.getString(KEY_SAVED_QUERY);
            mIsSearching = savedInstanceState.getBoolean(KEY_IS_SEARCHING);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        mFabView = findViewById(R.id.fab_button);
        mFabView.setOnClickListener(view -> showDetailFragment(null));
        mFabView.setOnLongClickListener(view -> {
            if (view.isHapticFeedbackEnabled()) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            Toast.makeText(view.getContext(), R.string.site_settings_tags_empty_button, Toast.LENGTH_SHORT).show();
            return true;
        });
        ViewUtilsKt.redirectContextClickToLongPressListener(mFabView);

        // hide the FAB the first time the fragment is created in order to animate it in onResume()
        if (savedInstanceState == null) {
            mFabView.setVisibility(View.INVISIBLE);
        }

        mActionableEmptyView = findViewById(R.id.actionable_empty_view);
        mActionableEmptyView.button.setOnClickListener(view -> showDetailFragment(null));

        mRecycler = findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setEmptyView(mActionableEmptyView);

        loadTags();

        if (savedInstanceState == null) {
            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(mSite));
        } else {
            SiteSettingsTagDetailFragment fragment = getDetailFragment();
            if (fragment != null) {
                fragment.setOnTagDetailListener(this);
            }
            if (savedInstanceState.containsKey(KEY_PROGRESS_RES_ID)) {
                @StringRes int messageId = savedInstanceState.getInt(KEY_PROGRESS_RES_ID);
                showProgressDialog(messageId);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showFabWithConditions();
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
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putBoolean(KEY_IS_SEARCHING, mIsSearching);

        if (mSearchMenuItem.isActionViewExpanded()) {
            outState.putString(KEY_SAVED_QUERY, mSearchView.getQuery().toString());
        }

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            outState.putInt(KEY_PROGRESS_RES_ID, mLastProgressResId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_list, menu);

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchMenuItem.setOnActionExpandListener(this);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        // open search bar if we were searching for something before
        if (mIsSearching) {
            mSearchMenuItem.expandActionView();
            mSearchView.clearFocus();
            mSearchView.setQuery(mSavedQuery, true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            SiteSettingsTagDetailFragment fragment = getDetailFragment();
            if (fragment != null && fragment.hasChanges()) {
                saveTag(fragment.getTerm(), fragment.isNewTerm());
            } else {
                hideDetailFragment();
                loadTags();
            }
        } else {
            super.onBackPressed();
        }
    }

    private void showFabIfHidden() {
        // redisplay hidden fab after a short delay
        long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
        new Handler().postDelayed(() -> {
            if (!isFinishing() && mFabView.getVisibility() != View.VISIBLE) {
                AniUtils.scaleIn(mFabView, AniUtils.Duration.MEDIUM);
            }
        }, delayMs);
    }

    private void showFabWithConditions() {
        // scale in the fab after a brief delay if it's not already showing
        if (mFabView.getVisibility() != View.VISIBLE) {
            long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
            new Handler().postDelayed(() -> {
                if (!mIsSearching && !isDetailFragmentShowing()
                    && mActionableEmptyView.getVisibility() != View.VISIBLE) {
                    showFabIfHidden();
                }
            }, delayMs);
        }
    }

    private void hideFabIfShowing() {
        if (!isFinishing() && mFabView.getVisibility() == View.VISIBLE) {
            AniUtils.scaleOut(mFabView, AniUtils.Duration.SHORT);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.SETTINGS, event.error.message);
        }
        switch (event.causeOfChange) {
            case FETCH_TAGS:
                if (!event.isError()) {
                    loadTags();
                }
                break;
            case REMOVE_TERM:
            case UPDATE_TERM:
                hideProgressDialog();
                hideDetailFragment();
                if (!event.isError()) {
                    loadTags();
                }
                break;
        }
    }

    private void loadTags() {
        List<TermModel> tags = mTaxonomyStore.getTagsForSite(mSite);
        Collections.sort(tags, (t1, t2) -> StringUtils.compareIgnoreCase(t1.getName(), t2.getName()));
        mAdapter = new TagListAdapter(tags);
        mRecycler.setAdapter(mAdapter);
    }

    private SiteSettingsTagDetailFragment getDetailFragment() {
        return (SiteSettingsTagDetailFragment) getFragmentManager()
                .findFragmentByTag(SiteSettingsTagDetailFragment.TAG);
    }

    /*
     * shows the detail (edit) view for the passed term, or adds a new term is passed term is null
     */
    private void showDetailFragment(@Nullable TermModel term) {
        SiteSettingsTagDetailFragment fragment = SiteSettingsTagDetailFragment.newInstance(term);
        fragment.setOnTagDetailListener(this);

        getFragmentManager().beginTransaction()
                            .add(R.id.container, fragment, SiteSettingsTagDetailFragment.TAG)
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .commitAllowingStateLoss();

        mSearchMenuItem.collapseActionView();
        mFabView.setVisibility(View.GONE);
    }

    private boolean isDetailFragmentShowing() {
        return getDetailFragment() != null;
    }

    private void hideDetailFragment() {
        SiteSettingsTagDetailFragment fragment = getDetailFragment();
        if (fragment != null) {
            getFragmentManager().popBackStack();
            ActivityUtils.hideKeyboard(this);
            showFabWithConditions();
            setTitle(R.string.site_settings_tags_title);
            invalidateOptionsMenu();
        }
    }

    private void showActionableEmptyViewForSearch(boolean isSearch) {
        mActionableEmptyView.updateLayoutForSearch(isSearch, 0);

        if (isSearch) {
            mActionableEmptyView.button.setVisibility(View.GONE);
            mActionableEmptyView.image.setVisibility(View.GONE);
            mActionableEmptyView.subtitle.setVisibility(View.GONE);
            mActionableEmptyView.title.setText(R.string.site_settings_tags_empty_title_search);
        } else {
            mActionableEmptyView.button.setVisibility(View.VISIBLE);
            mActionableEmptyView.image.setVisibility(View.VISIBLE);
            mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);
            mActionableEmptyView.title.setText(R.string.site_settings_tags_empty_title);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mAdapter.filter(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mAdapter.filter(query);
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mIsSearching = true;
        showActionableEmptyViewForSearch(true);
        hideFabIfShowing();
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mIsSearching = false;
        showActionableEmptyViewForSearch(false);
        showFabWithConditions();
        return true;
    }

    private void showProgressDialog(@StringRes int messageId) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getString(messageId));
        mProgressDialog.show();
        mLastProgressResId = messageId;
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onRequestDeleteTag(@NonNull TermModel term) {
        if (NetworkUtils.checkConnection(this)) {
            confirmDeleteTag(term);
        }
    }

    private void confirmDeleteTag(@NonNull final TermModel term) {
        String message = String.format(getString(R.string.dlg_confirm_delete_tag), term.getName());
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(
                getResources().getText(R.string.delete_yes),
                (dialog, whichButton) -> {
                    showProgressDialog(R.string.dlg_deleting_tag);
                    Action action = TaxonomyActionBuilder.newDeleteTermAction(
                            new TaxonomyStore.RemoteTermPayload(term, mSite));
                    mDispatcher.dispatch(action);
                });
        dialogBuilder.setNegativeButton(R.string.cancel, null);
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private void saveTag(@NonNull TermModel term, boolean isNewTerm) {
        if (isNewTerm && tagExists(term.getName())) {
            ToastUtils.showToast(this, R.string.error_tag_exists);
            return;
        }

        showProgressDialog(R.string.dlg_saving_tag);
        Action action = TaxonomyActionBuilder.newPushTermAction(new TaxonomyStore.RemoteTermPayload(term, mSite));
        mDispatcher.dispatch(action);
    }

    private boolean tagExists(@NonNull String termName) {
        List<TermModel> terms = mTaxonomyStore.getTagsForSite(mSite);
        for (TermModel term : terms) {
            if (termName.equalsIgnoreCase(term.getName())) {
                return true;
            }
        }
        return false;
    }

    private class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.TagViewHolder> {
        private final List<TermModel> mAllTags = new ArrayList<>();
        private final List<TermModel> mFilteredTags = new ArrayList<>();

        TagListAdapter(@NonNull List<TermModel> allTags) {
            mAllTags.addAll(allTags);
            mFilteredTags.addAll(allTags);
        }

        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.site_settings_tag_list_row, parent, false);
            return new TagListAdapter.TagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final TagListAdapter.TagViewHolder holder, int position) {
            TermModel term = mFilteredTags.get(position);
            holder.mTxtTag.setText(StringEscapeUtils.unescapeHtml4(term.getName()));
            if (term.getPostCount() > 0) {
                holder.mTxtCount.setVisibility(View.VISIBLE);
                holder.mTxtCount.setText(String.valueOf(term.getPostCount()));
            } else {
                holder.mTxtCount.setVisibility(View.GONE);
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
                    if (tag.getName().toLowerCase(Locale.getDefault())
                           .contains(text.toLowerCase(Locale.getDefault()))) {
                        mFilteredTags.add(tag);
                    }
                }
            }
            notifyDataSetChanged();

            showActionableEmptyViewForSearch(mIsSearching && mFilteredTags.isEmpty());
        }

        class TagViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTxtTag;
            private final TextView mTxtCount;

            TagViewHolder(View view) {
                super(view);
                mTxtTag = view.findViewById(R.id.text_tag);
                mTxtCount = view.findViewById(R.id.text_count);
                view.setOnClickListener(view1 -> {
                    if (!isDetailFragmentShowing()) {
                        int position = getAdapterPosition();
                        showDetailFragment(mFilteredTags.get(position));
                    }
                });
            }
        }
    }
}
