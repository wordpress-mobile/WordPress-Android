package org.wordpress.android.ui.history;

import android.app.Activity;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;

import java.util.ArrayList;

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
    private int mPosition;
    private boolean mIsChevronClicked = false;
    private boolean mIsFragmentRecreated = false;

    public static final String EXTRA_REVISION = "EXTRA_REVISION";
    public static final String EXTRA_REVISIONS = "EXTRA_REVISIONS";
    public static final String KEY_REVISION = "KEY_REVISION";

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

        if (getArguments() != null) {
            mRevision = getArguments().getParcelable(EXTRA_REVISION);
            mRevisions = getArguments().getParcelableArrayList(EXTRA_REVISIONS);
        }

        mIsFragmentRecreated = savedInstanceState != null;
        return rootView;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.revision_details, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.revision_load) {
            Intent intent = new Intent();
            intent.putExtra(KEY_REVISION, mRevision);

            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else if (item.getItemId() == R.id.revision_html_preview) {
            // TODO implement
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            mPosition = mRevisions.indexOf(mRevision);

            mViewPager = getView().findViewById(R.id.diff_pager);
            mViewPager.setPageTransformer(false, new WPViewPagerTransformer(TransformType.SLIDE_OVER));

            mAdapter = new HistoryDetailFragmentAdapter(getChildFragmentManager(), mRevisions);

            mViewPager.setAdapter(mAdapter);
            mViewPager.setCurrentItem(mPosition);

            mTotalAdditions = getView().findViewById(R.id.diff_additions);
            mTotalDeletions = getView().findViewById(R.id.diff_deletions);

            mNextButton = getView().findViewById(R.id.next);
            mNextButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View view) {
                    mIsChevronClicked = true;
                    mViewPager.setCurrentItem(mPosition + 1, true);
                }
            });

            mPreviousButton = getView().findViewById(R.id.previous);
            mPreviousButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View view) {
                    mIsChevronClicked = true;
                    mViewPager.setCurrentItem(mPosition - 1, true);
                }
            });

            refreshRevisionDetails();
            resetOnPageChangeListener();
        }
    }

    private void refreshRevisionDetails() {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(mRevision.getTimeSpan());
        }

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

        mNextButton.setEnabled(mPosition != mAdapter.getCount() - 1);
        mPreviousButton.setEnabled(mPosition != 0);
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
                    refreshRevisionDetails();
                }
            };
        }

        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    private class HistoryDetailFragmentAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<Revision> mRevisions;

        @SuppressWarnings("unchecked") HistoryDetailFragmentAdapter(FragmentManager fragmentManager,
                                                                    ArrayList<Revision> revisions) {
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
