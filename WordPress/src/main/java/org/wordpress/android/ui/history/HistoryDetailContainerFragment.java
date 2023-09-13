package org.wordpress.android.ui.history;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.HistoryDetailContainerFragmentBinding;
import org.wordpress.android.editor.EditorMediaUtils;
import org.wordpress.android.fluxc.model.revisions.RevisionModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.ui.posts.services.AztecImageLoader;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AniUtils.Duration;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;
import org.wordpress.aztec.plugins.IAztecPlugin;
import org.wordpress.aztec.plugins.shortcodes.AudioShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.CaptionShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.VideoShortcodePlugin;
import org.wordpress.aztec.plugins.wpcomments.HiddenGutenbergPlugin;
import org.wordpress.aztec.plugins.wpcomments.WordPressCommentsPlugin;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HistoryDetailContainerFragment extends Fragment {
    private ArrayList<Revision> mRevisions;
    private HistoryDetailFragmentAdapter mAdapter;
    private OnPageChangeListener mOnPageChangeListener;
    private Revision mRevision;
    private int mPosition;
    private boolean mIsChevronClicked = false;
    private boolean mIsFragmentRecreated = false;

    public static final String EXTRA_CURRENT_REVISION = "EXTRA_CURRENT_REVISION";
    public static final String EXTRA_PREVIOUS_REVISIONS_IDS = "EXTRA_PREVIOUS_REVISIONS_IDS";
    public static final String EXTRA_POST_ID = "EXTRA_POST_ID";
    public static final String EXTRA_SITE_ID = "EXTRA_SITE_ID";
    public static final String KEY_REVISION = "KEY_REVISION";
    public static final String KEY_IS_IN_VISUAL_PREVIEW = "KEY_IS_IN_VISUAL_PREVIEW";

    @Inject ImageManager mImageManager;

    @Inject PostStore mPostStore;

    @Nullable private HistoryDetailContainerFragmentBinding mBinding = null;

    public static HistoryDetailContainerFragment newInstance(final Revision revision,
                                                             final long[] previousRevisionsIds,
                                                             final long postId,
                                                             final long siteId) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CURRENT_REVISION, revision);
        args.putLongArray(EXTRA_PREVIOUS_REVISIONS_IDS, previousRevisionsIds);
        args.putLong(EXTRA_POST_ID, postId);
        args.putLong(EXTRA_SITE_ID, siteId);
        HistoryDetailContainerFragment fragment = new HistoryDetailContainerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = HistoryDetailContainerFragmentBinding.inflate(inflater, container, false);

        mIsFragmentRecreated = savedInstanceState != null;

        mapRevisions();

        if (mRevisions != null) {
            for (final Revision revision : mRevisions) {
                if (revision.getRevisionId() == mRevision.getRevisionId()) {
                    mPosition = mRevisions.indexOf(revision);
                }
            }
        } else {
            throw new IllegalArgumentException("Revisions list extra is null in HistoryDetailContainerFragment");
        }

        mAdapter = new HistoryDetailFragmentAdapter(getChildFragmentManager(), mRevisions);

        if (mBinding != null) {
            mBinding.diffPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.SLIDE_OVER));
            mBinding.diffPager.setAdapter(mAdapter);
            mBinding.diffPager.setCurrentItem(mPosition);

            mBinding.next.setOnClickListener(view -> {
                mIsChevronClicked = true;
                mBinding.diffPager.setCurrentItem(mPosition + 1, true);
            });
            mBinding.previous.setOnClickListener(view -> {
                mIsChevronClicked = true;
                mBinding.diffPager.setCurrentItem(mPosition - 1, true);
            });

            Drawable loadingImagePlaceholder = EditorMediaUtils.getAztecPlaceholderDrawableFromResID(
                    requireContext(),
                    org.wordpress.android.editor.R.drawable.ic_gridicons_image,
                    EditorMediaUtils.getMaximumThumbnailSizeForEditor(requireContext()));

            mBinding.visualContent.setImageGetter(
                    new AztecImageLoader(requireContext(),
                            mImageManager,
                            loadingImagePlaceholder)
            );
            mBinding.visualContent.setKeyListener(null);
            mBinding.visualContent.setTextIsSelectable(true);
            mBinding.visualContent.setCursorVisible(false);
            mBinding.visualContent.setMovementMethod(LinkMovementMethod.getInstance());

            ArrayList<IAztecPlugin> plugins = new ArrayList<>();
            plugins.add(new WordPressCommentsPlugin(mBinding.visualContent));
            plugins.add(new CaptionShortcodePlugin(mBinding.visualContent));
            plugins.add(new VideoShortcodePlugin());
            plugins.add(new AudioShortcodePlugin());
            plugins.add(new HiddenGutenbergPlugin(mBinding.visualContent));
            mBinding.visualContent.setPlugins(plugins);

            boolean isInVisualPreview = savedInstanceState != null
                                        && savedInstanceState.getBoolean(KEY_IS_IN_VISUAL_PREVIEW);
            mBinding.visualPreviewContainer.setVisibility(isInVisualPreview ? View.VISIBLE : View.GONE);
            mBinding.diffPager.setVisibility(isInVisualPreview ? View.GONE : View.VISIBLE);
            mBinding.previous.setVisibility(isInVisualPreview ? View.INVISIBLE : View.VISIBLE);
            mBinding.next.setVisibility(isInVisualPreview ? View.INVISIBLE : View.VISIBLE);
        }

        refreshHistoryDetail();
        resetOnPageChangeListener();

        return mBinding.getRoot();
    }

    private void mapRevisions() {
        if (getArguments() != null) {
            mRevision = getArguments().getParcelable(EXTRA_CURRENT_REVISION);

            final long[] previousRevisionsIds = getArguments().getLongArray(EXTRA_PREVIOUS_REVISIONS_IDS);
            final List<RevisionModel> revisionModels = new ArrayList<>();
            final long postId = getArguments().getLong(EXTRA_POST_ID);
            final long siteId = getArguments().getLong(EXTRA_SITE_ID);
            for (final long revisionId : previousRevisionsIds) {
                revisionModels.add(mPostStore.getRevisionById(revisionId, postId, siteId));
            }
            mRevisions = mapRevisionModelsToRevisions(revisionModels);
        }
    }

    @Nullable
    private ArrayList<Revision> mapRevisionModelsToRevisions(@Nullable final List<RevisionModel> revisionModels) {
        if (revisionModels == null) {
            return null;
        }
        final ArrayList<Revision> revisions = new ArrayList<>();
        for (int i = 0; i < revisionModels.size(); i++) {
            final RevisionModel current = revisionModels.get(i);
            revisions.add(new Revision(current));
        }
        return revisions;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showHistoryTimeStampInToolbar();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_IN_VISUAL_PREVIEW, isInVisualPreview());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_detail, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem viewMode = menu.findItem(R.id.history_toggle_view);
        viewMode.setTitle(isInVisualPreview() ? R.string.history_preview_html : R.string.history_preview_visual);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.history_load) {
            Intent intent = new Intent();
            intent.putExtra(KEY_REVISION, mRevision);

            requireActivity().setResult(Activity.RESULT_OK, intent);
            requireActivity().finish();
        } else if (item.getItemId() == R.id.history_toggle_view) {
            if (mBinding != null) {
                if (isInVisualPreview()) {
                    AniUtils.fadeIn(mBinding.next, Duration.SHORT);
                    AniUtils.fadeIn(mBinding.previous, Duration.SHORT);
                } else {
                    String title = TextUtils.isEmpty(mRevision.getPostTitle())
                            ? getString(R.string.history_no_title) : mRevision.getPostTitle();
                    mBinding.visualTitle.setText(title);
                    mBinding.visualContent.fromHtml(StringUtils.notNullStr(mRevision.getPostContent()), false);
                    AniUtils.fadeOut(mBinding.next, Duration.SHORT, View.INVISIBLE);
                    AniUtils.fadeOut(mBinding.previous, Duration.SHORT, View.INVISIBLE);
                }
                crossfadePreviewViews();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void crossfadePreviewViews() {
        if (mBinding != null) {
            final View fadeInView = isInVisualPreview() ? mBinding.diffPager : mBinding.visualPreviewContainer;
            final View fadeOutView = isInVisualPreview() ? mBinding.visualPreviewContainer : mBinding.diffPager;
            mBinding.visualPreviewContainer.smoothScrollTo(0, 0);
            mBinding.visualPreviewContainer.post(new Runnable() {
                @Override
                public void run() {
                    AniUtils.fadeIn(fadeInView, Duration.SHORT);
                    AniUtils.fadeOut(fadeOutView, Duration.SHORT);
                }
            });
        }
    }

    private void showHistoryTimeStampInToolbar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(mRevision.getTimeSpan());
        }
    }

    private void refreshHistoryDetail() {
        if (mBinding != null) {
            if (mRevision.getTotalAdditions() > 0) {
                mBinding.diffAdditions.setText(String.valueOf(mRevision.getTotalAdditions()));
                mBinding.diffAdditions.setVisibility(View.VISIBLE);
            } else {
                mBinding.diffAdditions.setVisibility(View.GONE);
            }

            if (mRevision.getTotalDeletions() > 0) {
                mBinding.diffDeletions.setText(String.valueOf(mRevision.getTotalDeletions()));
                mBinding.diffDeletions.setVisibility(View.VISIBLE);
            } else {
                mBinding.diffDeletions.setVisibility(View.GONE);
            }

            mBinding.previous.setEnabled(mPosition != 0);
            mBinding.next.setEnabled(mPosition != mAdapter.getCount() - 1);
        }
    }

    private void resetOnPageChangeListener() {
        if (mBinding != null) {
            if (mOnPageChangeListener != null) {
                mBinding.diffPager.removeOnPageChangeListener(mOnPageChangeListener);
            } else {
                mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }

                    @Override
                    public void onPageSelected(int position) {
                        if (mIsChevronClicked) {
                            AnalyticsTracker.track(Stat.REVISIONS_DETAIL_VIEWED_FROM_CHEVRON);
                            mIsChevronClicked = false;
                        } else {
                            if (!mIsFragmentRecreated) {
                                AnalyticsTracker.track(Stat.REVISIONS_DETAIL_VIEWED_FROM_SWIPE);
                            } else {
                                mIsFragmentRecreated = false;
                            }
                        }

                        mPosition = position;
                        mRevision = mAdapter.getRevisionAtPosition(mPosition);
                        refreshHistoryDetail();
                        showHistoryTimeStampInToolbar();
                    }
                };
            }

            mBinding.diffPager.addOnPageChangeListener(mOnPageChangeListener);
        }
    }

    private boolean isInVisualPreview() {
        if (mBinding != null) {
            return mBinding.visualPreviewContainer.getVisibility() == View.VISIBLE;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private class HistoryDetailFragmentAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<Revision> mRevisions;

        @SuppressWarnings("unchecked")
        HistoryDetailFragmentAdapter(FragmentManager fragmentManager, ArrayList<Revision> revisions) {
            super(fragmentManager);
            mRevisions = (ArrayList<Revision>) revisions.clone();
        }

        @Override
        public Fragment getItem(int position) {
            return HistoryDetailFragment.Companion.newInstance(mRevisions.get(position));
        }

        @Override
        public int getCount() {
            return mRevisions.size();
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException exception) {
                AppLog.e(T.EDITOR, exception);
            }
        }

        @Override
        public Parcelable saveState() {
            Bundle bundle = (Bundle) super.saveState();

            if (bundle == null) {
                bundle = new Bundle();
            }

            bundle.putParcelableArray("states", null);
            return bundle;
        }

        private Revision getRevisionAtPosition(int position) {
            if (isValidPosition(position)) {
                return mRevisions.get(position);
            } else {
                return null;
            }
        }

        private boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }
    }
}
