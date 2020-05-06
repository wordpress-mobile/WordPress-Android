package org.wordpress.android.ui.accounts.login;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.wordpress.android.R;

public class LoginProloguePagerAdapter extends FragmentPagerAdapter {
    static final String LOGIN_PROLOGUE_JETPACK_TAG = "login_prologue_jetpack_tag";
    static final String LOGIN_PROLOGUE_NOTIFICATIONS_TAG = "login_prologue_notifications_tag";
    static final String LOGIN_PROLOGUE_POST_TAG = "login_prologue_post_tag";
    static final String LOGIN_PROLOGUE_STATS_TAG = "login_prologue_stats_tag";
    static final String LOGIN_PROLOGUE_READER_TAG = "login_prologue_reader_tag";

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

    private static final String[] TAGS = {
            LOGIN_PROLOGUE_POST_TAG,
            LOGIN_PROLOGUE_STATS_TAG,
            LOGIN_PROLOGUE_READER_TAG,
            LOGIN_PROLOGUE_NOTIFICATIONS_TAG,
            LOGIN_PROLOGUE_JETPACK_TAG
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

    public static String getTag(int position) {
        return TAGS[position];
    }
}
