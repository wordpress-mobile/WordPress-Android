package org.wordpress.android.ui.accounts.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

import org.wordpress.android.R;

public class LoginPrologueAnimationFragment extends Fragment {
    private static final String KEY_ANIMATION_FILENAME = "KEY_ANIMATION_FILENAME";
    private static final String KEY_PROMO_TITLE = "KEY_PROMO_TITLE";
    private static final String KEY_PROMO_TEXT = "KEY_PROMO_TEXT";

    private LottieAnimationView mLottieAnimationView;

    private String mAnimationFilename;
    private @StringRes int mPromoTitle;
    private @StringRes int mPromoText;

    static LoginPrologueAnimationFragment newInstance(String animationFilename, @StringRes int promoTitle,
                                                      @StringRes int promoText) {
        LoginPrologueAnimationFragment fragment = new LoginPrologueAnimationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ANIMATION_FILENAME, animationFilename);
        bundle.putInt(KEY_PROMO_TITLE, promoTitle);
        bundle.putInt(KEY_PROMO_TEXT, promoText);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAnimationFilename = getArguments().getString(KEY_ANIMATION_FILENAME);
        mPromoTitle = getArguments().getInt(KEY_PROMO_TITLE);
        mPromoText = getArguments().getInt(KEY_PROMO_TEXT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_intro_template_view, container, false);

        TextView promoTitle = rootView.findViewById(R.id.promo_title);
        promoTitle.setText(mPromoTitle);

        TextView promoText = rootView.findViewById(R.id.promo_text);
        promoText.setText(mPromoText);

//        mLottieAnimationView = rootView.findViewById(R.id.animation_view);
//        mLottieAnimationView.setAnimation(mAnimationFilename);

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // toggle the animation but only if already resumed.
        // Needed because setUserVisibleHint is called before onCreateView
        if (isResumed()) {
            // toggleAnimation(isVisibleToUser);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // need to toggle the animation so the first time the fragment is resumed it starts animating (if visible).
        // toggleAnimation(getUserVisibleHint());
    }

    private void toggleAnimation(boolean isVisibleToUser) {
        if (isVisibleToUser) {
            mLottieAnimationView.playAnimation();
        } else {
            mLottieAnimationView.cancelAnimation();
            mLottieAnimationView.setProgress(0);
        }
    }
}
