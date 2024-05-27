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
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase;
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
    @Nullable private OnPageChangeListener mOnPageChangeListener;
    @Nullable private Revision mRevision;
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

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        mBinding = HistoryDetailContainerFragmentBinding.inflate(inflater, container, false);

        mIsFragmentRecreated = savedInstanceState != null;

        ArrayList<Revision> revisions = mapRevisions();
        if (revisions != null && mRevision != null) {
            for (final Revision revision : revisions) {
                if (revision.getRevisionId() == mRevision.getRevisionId()) {
                    mPosition = revisions.indexOf(revision);
                }
            }
        } else {
            throw new IllegalArgumentException("Revisions list extra is null in HistoryDetailContainerFragment");
        }

        HistoryDetailFragmentAdapter adapter = new HistoryDetailFragmentAdapter(getChildFragmentManager(), revisions);

        if (mBinding != null && mRevision != null) {
            mBinding.diffPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.SLIDE_OVER));
            mBinding.diffPager.setAdapter(adapter);
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

            refreshHistoryDetail(mBinding, adapter, mRevision);
            resetOnPageChangeListener(mBinding, adapter);

            return mBinding.getRoot();
        } else {
            throw new IllegalStateException("mBinding or mRevision is null");
        }
    }

    @Nullable
    private ArrayList<Revision> mapRevisions() {
        if (getArguments() != null) {
            mRevision = getArguments().getParcelable(EXTRA_CURRENT_REVISION);

            final long[] previousRevisionsIds = getArguments().getLongArray(EXTRA_PREVIOUS_REVISIONS_IDS);
            if (previousRevisionsIds == null) {
                return null;
            }
            final List<RevisionModel> revisionModels = new ArrayList<>();
            final long postId = getArguments().getLong(EXTRA_POST_ID);
            final long siteId = getArguments().getLong(EXTRA_SITE_ID);
            for (final long revisionId : previousRevisionsIds) {
                revisionModels.add(mPostStore.getRevisionById(revisionId, postId, siteId));
            }
            return mapRevisionModelsToRevisions(revisionModels);
        } else {
            return null;
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
            if (current != null) {
                revisions.add(new Revision(current));
            }
        }
        return revisions;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mRevision != null) {
            showHistoryTimeStampInToolbar(mRevision);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mBinding != null) {
            outState.putBoolean(KEY_IS_IN_VISUAL_PREVIEW, isInVisualPreview(mBinding));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_detail, menu);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mBinding != null) {
            MenuItem viewMode = menu.findItem(R.id.history_toggle_view);
            viewMode.setTitle(
                    isInVisualPreview(mBinding) ? R.string.history_preview_html : R.string.history_preview_visual
            );
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.history_load) {
            Intent intent = new Intent();
            SavedInstanceDatabase db = SavedInstanceDatabase.Companion.getDatabase(WordPress.getContext());
            if (db != null) {
                db.addParcel(KEY_REVISION, mRevision);
            }

            requireActivity().setResult(Activity.RESULT_OK, intent);
            requireActivity().finish();
        } else if (item.getItemId() == R.id.history_toggle_view) {
            if (mBinding != null && mRevision != null) {
                if (isInVisualPreview(mBinding)) {
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
                crossfadePreviewViews(mBinding);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void crossfadePreviewViews(@NonNull HistoryDetailContainerFragmentBinding binding) {
        final View fadeInView = isInVisualPreview(binding) ? binding.diffPager : binding.visualPreviewContainer;
        final View fadeOutView = isInVisualPreview(binding) ? binding.visualPreviewContainer : binding.diffPager;
        binding.visualPreviewContainer.smoothScrollTo(0, 0);
        binding.visualPreviewContainer.post(() -> {
            AniUtils.fadeIn(fadeInView, Duration.SHORT);
            AniUtils.fadeOut(fadeOutView, Duration.SHORT);
        });
    }

    private void showHistoryTimeStampInToolbar(@NonNull Revision revision) {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(revision.getTimeSpan());
        }
    }

    private void refreshHistoryDetail(
            @NonNull HistoryDetailContainerFragmentBinding binding,
            @NonNull HistoryDetailFragmentAdapter adapter,
            @NonNull Revision revision
    ) {
        if (revision.getTotalAdditions() > 0) {
            binding.diffAdditions.setText(String.valueOf(revision.getTotalAdditions()));
            binding.diffAdditions.setVisibility(View.VISIBLE);
        } else {
            binding.diffAdditions.setVisibility(View.GONE);
        }

        if (revision.getTotalDeletions() > 0) {
            binding.diffDeletions.setText(String.valueOf(revision.getTotalDeletions()));
            binding.diffDeletions.setVisibility(View.VISIBLE);
        } else {
            binding.diffDeletions.setVisibility(View.GONE);
        }

        binding.previous.setEnabled(mPosition != 0);
        binding.next.setEnabled(mPosition != adapter.getCount() - 1);
    }

    private void resetOnPageChangeListener(
            @NonNull HistoryDetailContainerFragmentBinding binding,
            @NonNull HistoryDetailFragmentAdapter adapter
    ) {
        if (mOnPageChangeListener != null) {
            binding.diffPager.removeOnPageChangeListener(mOnPageChangeListener);
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
                    mRevision = adapter.getRevisionAtPosition(mPosition);
                    if (mRevision != null) {
                        refreshHistoryDetail(binding, adapter, mRevision);
                        showHistoryTimeStampInToolbar(mRevision);
                    }
                }
            };
        }

        binding.diffPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    private boolean isInVisualPreview(@NonNull HistoryDetailContainerFragmentBinding binding) {
        return binding.visualPreviewContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @SuppressWarnings("deprecation")
    private static class HistoryDetailFragmentAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<Revision> mRevisions;

        @SuppressWarnings({"unchecked", "deprecation"})
        HistoryDetailFragmentAdapter(FragmentManager fragmentManager, ArrayList<Revision> revisions) {
            super(fragmentManager);
            mRevisions = (ArrayList<Revision>) revisions.clone();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return HistoryDetailFragment.Companion.newInstance(mRevisions.get(position));
        }

        @Override
        public int getCount() {
            return mRevisions.size();
        }

        @Override
        public void restoreState(@Nullable Parcelable state, @Nullable ClassLoader loader) {
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException exception) {
                AppLog.e(T.EDITOR, exception);
            }
        }

        @Nullable
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
