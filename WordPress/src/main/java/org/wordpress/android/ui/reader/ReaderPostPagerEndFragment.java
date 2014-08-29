package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

/**
 * fragment that appears when user scrolls beyond the last post in ReaderPostPagerActivity
 **/

public class ReaderPostPagerEndFragment extends Fragment {
    static final long END_FRAGMENT_ID = -1;
    static enum EndFragmentType { LOADING, NO_MORE }
    private static final String ARG_END_FRAGMENT_TYPE = "end_fragment_type";

    private EndFragmentType mFragmentType = EndFragmentType.LOADING;

    static ReaderPostPagerEndFragment newInstance(EndFragmentType fragmentType) {
        ReaderPostPagerEndFragment fragment = new ReaderPostPagerEndFragment();
        Bundle bundle = new Bundle();
        if (fragmentType != null) {
            bundle.putSerializable(ARG_END_FRAGMENT_TYPE, fragmentType);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null && args.containsKey(ARG_END_FRAGMENT_TYPE)) {
            mFragmentType = (EndFragmentType) args.getSerializable(ARG_END_FRAGMENT_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_pager_end, container, false);
        setEndFragmentType(view, mFragmentType);
        return view;
    }

    void setEndFragmentType(EndFragmentType fragmentType) {
        setEndFragmentType(getView(), fragmentType);
    }
    void setEndFragmentType(View view, EndFragmentType fragmentType) {
        mFragmentType = fragmentType;
        AppLog.d(AppLog.T.READER, "reader pager > setting end fragment type to " + fragmentType.toString());

        if (view == null) {
            AppLog.w(AppLog.T.READER, "reader pager > null view setting end fragment type");
            return;
        }

        ViewGroup layoutLoading = (ViewGroup) view.findViewById(R.id.layout_loading);
        ViewGroup layoutNoMore = (ViewGroup) view.findViewById(R.id.layout_no_more);

        switch (mFragmentType) {
            // indicates that more posts can be loaded - request to get older posts
            // will occur when this page becomes active
            case LOADING:
                layoutLoading.setVisibility(View.VISIBLE);
                layoutNoMore.setVisibility(View.GONE);
                break;
            // indicates the user has reached the end (there are no more posts) - tapping
            // this will return to the previous activity
            case NO_MORE:
                layoutLoading.setVisibility(View.GONE);
                layoutNoMore.setVisibility(View.VISIBLE);
                layoutNoMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        // setUserVisibleHint wasn't available until API 15 (ICE_CREAM_SANDWICH_MR1)
        if (Build.VERSION.SDK_INT >= 15) {
            super.setUserVisibleHint(isVisibleToUser);
        }
        // animate in the checkmark if this is a "no more posts fragment
        if (mFragmentType == EndFragmentType.NO_MORE) {
            animateCheckmark();
        }
    }

    private void animateCheckmark() {
        if (!isAdded() || getView() == null) {
            return;
        }

        // don't animate checkmark if it's already visible
        final TextView txtCheckmark = (TextView) getView().findViewById(R.id.text_checkmark);
        if (txtCheckmark == null || txtCheckmark.getVisibility() == View.VISIBLE) {
            return;
        }

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.25f, 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.25f, 1.0f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(txtCheckmark, scaleX, scaleY);
        animator.setDuration(750);
        animator.setInterpolator(new OvershootInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                txtCheckmark.setVisibility(View.VISIBLE);
            }
        });

        animator.start();
    }
}