package org.wordpress.android.ui.accounts.login;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.wordpress.android.R;

public class LoginProloguePagerAdapter extends FragmentPagerAdapter {

    private final String[] mAnims = {
            "login_anims/post.json",
            "login_anims/stats.json",
            "login_anims/reader.json",
            "login_anims/notifications.json",
            "login_anims/jetpack.json"
    };

    private final @StringRes int[] mPromoTexts = {
        R.string.login_promo_text_onthego,
        R.string.login_promo_text_realtime,
        R.string.login_promo_text_anytime,
        R.string.login_promo_text_notifications,
        R.string.login_promo_text_jetpack
    };

    public LoginProloguePagerAdapter(FragmentManager supportFragmentManager) {
        super(supportFragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        return LoginPrologueAnimationFragment.newInstance(mAnims[position], mPromoTexts[position], (position % 2) == 0);
    }

    @Override
    public int getCount() {
        return mPromoTexts.length;
    }
}
