package org.wordpress.android.ui.history;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.FullScreenDialogFragment;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController;
import org.wordpress.android.ui.history.HistoryListItem.Revision;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.android.widgets.WPViewPagerTransformer;
import org.wordpress.android.widgets.WPViewPagerTransformer.TransformType;

import java.util.ArrayList;

public class HistoryDetailFullScreenDialogFragment extends Fragment implements FullScreenDialogContent {
    protected FullScreenDialogController mDialogController;

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

    public static final String EXTRA_REVISION = "EXTRA_REVISION";
    public static final String EXTRA_REVISIONS = "EXTRA_REVISIONS";
    public static final String KEY_REVISION = "KEY_REVISION";

    public static Bundle newBundle(Revision revision, ArrayList<Revision> revisions) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_REVISION, revision);
        bundle.putParcelableArrayList(EXTRA_REVISIONS, revisions);
        return bundle;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.history_detail_dialog_fragment, container, false);

        if (getArguments() != null) {
            mRevision = getArguments().getParcelable(EXTRA_REVISION);
            mRevisions = getArguments().getParcelableArrayList(EXTRA_REVISIONS);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(final FullScreenDialogController controller) {
        mDialogController = controller;
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
                    mViewPager.setCurrentItem(mPosition + 1, true);
                }
            });

            mPreviousButton = getView().findViewById(R.id.previous);
            mPreviousButton.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View view) {
                    mViewPager.setCurrentItem(mPosition - 1, true);
                }
            });

            refreshRevisionDetails();
            resetOnPageChangeListener();
        }
    }

    @Override
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        // TODO: Add analytics tracking for confirm button.
        Bundle result = new Bundle();
        result.putParcelable(KEY_REVISION, mRevisions.get(mPosition));
        controller.confirm(result);
        return true;
    }

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        controller.dismiss();
        return true;
    }

    private void refreshRevisionDetails() {
        if (getParentFragment() instanceof FullScreenDialogFragment) {
            ((FullScreenDialogFragment) getParentFragment()).setSubtitle(mRevision.getTimeSpan());
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
