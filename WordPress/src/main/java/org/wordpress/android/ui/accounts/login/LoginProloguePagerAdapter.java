package org.wordpress.android.ui.accounts.login;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.wordpress.android.R;

public class LoginProloguePagerAdapter extends FragmentPagerAdapter {
    private final String[] mAnims = {
            "login_anims/post.json",
            "login_anims/stats.json",
            "login_anims/reader.json",
            "login_anims/notifications.json",
            "login_anims/jetpack.json"
    };

    private final @StringRes int[] mPromoTitles = {
            R.string.login_promo_title_36_percent,
            R.string.login_promo_title_36_percent,
            R.string.login_promo_title_36_percent,
            R.string.login_promo_title_36_percent,
            R.string.login_promo_title_36_percent
    };

    private final @StringRes int[] mPromoTexts = {
            R.string.login_promo_text_unlock_the_power,
            R.string.login_promo_text_unlock_the_power,
            R.string.login_promo_text_unlock_the_power,
            R.string.login_promo_text_unlock_the_power,
            R.string.login_promo_text_unlock_the_power
    };

    public LoginProloguePagerAdapter(FragmentManager supportFragmentManager) {
        super(supportFragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        return LoginPrologueAnimationFragment.newInstance(mAnims[position], mPromoTitles[position], mPromoTexts[position]);
    }

    @Override
    public int getCount() {
        return mPromoTexts.length;
    }
}
