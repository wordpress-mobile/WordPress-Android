package org.wordpress.android.ui.history;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.EditorMediaUtils;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.ui.posts.services.AztecImageLoader;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AniUtils.Duration;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;
import org.wordpress.aztec.AztecText;
import org.wordpress.aztec.plugins.IAztecPlugin;
import org.wordpress.aztec.plugins.shortcodes.AudioShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.CaptionShortcodePlugin;
import org.wordpress.aztec.plugins.shortcodes.VideoShortcodePlugin;
import org.wordpress.aztec.plugins.wpcomments.HiddenGutenbergPlugin;
import org.wordpress.aztec.plugins.wpcomments.WordPressCommentsPlugin;

import java.util.ArrayList;

import javax.inject.Inject;

public class HistoryDetailContainerFragment extends Fragment {
    private ArrayList<Revision> mRevisions;
    private HistoryDetailFragmentAdapter mAdapter;
    private ImageView mNextButton;
    private ImageView mPreviousButton;
    private OnPageChangeListener mOnPageChangeListener;
    private Revision mRevision;
    private TextView mTotalAdditions;
    private TextView mTotalDeletions;
    private WPViewPager mViewPager;
    private AztecText mVisualContent;
    private TextView mVisualTitle;
    private ScrollView mVisualPreviewContainer;
    private int mPosition;
    private boolean mIsChevronClicked = false;
    private boolean mIsFragmentRecreated = false;

    public static final String EXTRA_REVISION = "EXTRA_REVISION";
    public static final String EXTRA_REVISIONS = "EXTRA_REVISIONS";
    public static final String KEY_REVISION = "KEY_REVISION";
    public static final String KEY_IS_IN_VISUAL_PREVIEW = "KEY_IS_IN_VISUAL_PREVIEW";

    @Inject ImageManager mImageManager;

    public static HistoryDetailContainerFragment newInstance(Revision revision, ArrayList<Revision> revisions) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_REVISION, revision);
        args.putParcelableArrayList(EXTRA_REVISIONS, revisions);
        HistoryDetailContainerFragment fragment = new HistoryDetailContainerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.history_detail_container_fragment, container, false);

        mIsFragmentRecreated = savedInstanceState != null;

        if (getArguments() != null) {
            mRevision = getArguments().getParcelable(EXTRA_REVISION);
            mRevisions = getArguments().getParcelableArrayList(EXTRA_REVISIONS);
        }

        if (mRevisions != null) {
            mPosition = mRevisions.indexOf(mRevision);
        } else {
            throw new IllegalArgumentException("Revisions list extra is null in HistoryDetailContainerFragment");
        }

        mViewPager = rootView.findViewById(R.id.diff_pager);
        mViewPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.SLIDE_OVER));

        mAdapter = new HistoryDetailFragmentAdapter(getChildFragmentManager(), mRevisions);

        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(mPosition);

        mTotalAdditions = rootView.findViewById(R.id.diff_additions);
        mTotalDeletions = rootView.findViewById(R.id.diff_deletions);

        mNextButton = rootView.findViewById(R.id.next);
        mNextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsChevronClicked = true;
                mViewPager.setCurrentItem(mPosition + 1, true);
            }
        });

        mPreviousButton = rootView.findViewById(R.id.previous);
        mPreviousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsChevronClicked = true;
                mViewPager.setCurrentItem(mPosition - 1, true);
            }
        });

        mVisualTitle = rootView.findViewById(R.id.visual_title);
        mVisualContent = rootView.findViewById(R.id.visual_content);

        Drawable loadingImagePlaceholder = EditorMediaUtils.getAztecPlaceholderDrawableFromResID(
                requireContext(),
                org.wordpress.android.editor.R.drawable.ic_gridicons_image,
                EditorMediaUtils.getMaximumThumbnailSizeForEditor(requireContext()));

        mVisualContent.setImageGetter(new AztecImageLoader(requireContext(), mImageManager, loadingImagePlaceholder));
        mVisualContent.setKeyListener(null);
        mVisualContent.setTextIsSelectable(true);
        mVisualContent.setCursorVisible(false);
        mVisualContent.setMovementMethod(LinkMovementMethod.getInstance());

        ArrayList<IAztecPlugin> plugins = new ArrayList<>();
        plugins.add(new WordPressCommentsPlugin(mVisualContent));
        plugins.add(new CaptionShortcodePlugin(mVisualContent));
        plugins.add(new VideoShortcodePlugin());
        plugins.add(new AudioShortcodePlugin());
        plugins.add(new HiddenGutenbergPlugin(mVisualContent));
        mVisualContent.setPlugins(plugins);

        mVisualPreviewContainer = rootView.findViewById(R.id.visual_preview_container);

        boolean isInVisualPreview = savedInstanceState != null
                                    && savedInstanceState.getBoolean(KEY_IS_IN_VISUAL_PREVIEW);
        mVisualPreviewContainer.setVisibility(isInVisualPreview ? View.VISIBLE : View.GONE);
        mViewPager.setVisibility(isInVisualPreview ? View.GONE : View.VISIBLE);
        mPreviousButton.setVisibility(isInVisualPreview ? View.INVISIBLE : View.VISIBLE);
        mNextButton.setVisibility(isInVisualPreview ? View.INVISIBLE : View.VISIBLE);

        refreshHistoryDetail();
        resetOnPageChangeListener();

        return rootView;
    }

    @Override
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_detail, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem viewMode = menu.findItem(R.id.history_toggle_view);
        viewMode.setTitle(isInVisualPreview() ? R.string.history_preview_html : R.string.history_preview_visual);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.history_load) {
            Intent intent = new Intent();
            intent.putExtra(KEY_REVISION, mRevision);

            requireActivity().setResult(Activity.RESULT_OK, intent);
            requireActivity().finish();
        } else if (item.getItemId() == R.id.history_toggle_view) {
            if (isInVisualPreview()) {
                AniUtils.fadeIn(mNextButton, Duration.SHORT);
                AniUtils.fadeIn(mPreviousButton, Duration.SHORT);
            } else {
                String title = TextUtils.isEmpty(mRevision.getPostTitle())
                        ? getString(R.string.history_no_title) : mRevision.getPostTitle();
                mVisualTitle.setText(title);
                mVisualContent.fromHtml(StringUtils.notNullStr(mRevision.getPostContent()), false);
                AniUtils.fadeOut(mNextButton, Duration.SHORT, View.INVISIBLE);
                AniUtils.fadeOut(mPreviousButton, Duration.SHORT, View.INVISIBLE);
            }
            crossfadePreviewViews();
        }
        return super.onOptionsItemSelected(item);
    }

    private void crossfadePreviewViews() {
        final View fadeInView = isInVisualPreview() ? mViewPager : mVisualPreviewContainer;
        final View fadeOutView = isInVisualPreview() ? mVisualPreviewContainer : mViewPager;
        mVisualPreviewContainer.smoothScrollTo(0, 0);
        mVisualPreviewContainer.post(new Runnable() {
            @Override
            public void run() {
                AniUtils.fadeIn(fadeInView, Duration.SHORT);
                AniUtils.fadeOut(fadeOutView, Duration.SHORT);
            }
        });
    }

    private void showHistoryTimeStampInToolbar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(mRevision.getTimeSpan());
        }
    }

    private void refreshHistoryDetail() {
        if (mRevision.getTotalAdditions() > 0) {
            mTotalAdditions.setText(String.valueOf(mRevision.getTotalAdditions()));
            mTotalAdditions.setVisibility(View.VISIBLE);
        } else {
            mTotalAdditions.setVisibility(View.GONE);
        }

        if (mRevision.getTotalDeletions() > 0) {
            mTotalDeletions.setText(String.valueOf(mRevision.getTotalDeletions()));
            mTotalDeletions.setVisibility(View.VISIBLE);
        } else {
            mTotalDeletions.setVisibility(View.GONE);
        }

        mPreviousButton.setEnabled(mPosition != 0);
        mNextButton.setEnabled(mPosition != mAdapter.getCount() - 1);
    }

    private void resetOnPageChangeListener() {
        if (mOnPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mOnPageChangeListener);
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

        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    private boolean isInVisualPreview() {
        return mVisualPreviewContainer.getVisibility() == View.VISIBLE;
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
