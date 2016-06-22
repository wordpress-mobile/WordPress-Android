package org.wordpress.android.util;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppSettingsFragment;

import java.util.List;
import java.util.Locale;

public class WPActivityUtils {
    // Hack! PreferenceScreens don't show the toolbar, so we'll manually add one
    // See: http://stackoverflow.com/a/27455363/309558
    public static void addToolbarToDialog(final Fragment context, final Dialog dialog, String title) {
        if (!context.isAdded() || dialog == null) {
            return;
        }

        Toolbar toolbar;
        if (dialog.findViewById(android.R.id.list) == null) {
            return;
        }

        ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.list).getParent();
        toolbar = (Toolbar) LayoutInflater.from(context.getActivity())
                .inflate(org.wordpress.android.R.layout.toolbar, root, false);
        root.addView(toolbar, 0);

        dialog.getWindow().setWindowAnimations(R.style.DialogAnimations);

        TextView titleView = (TextView) toolbar.findViewById(R.id.toolbar_title);
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(title);

        toolbar.setTitle("");
        toolbar.setNavigationIcon(org.wordpress.android.R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Checks for a {@link Toolbar} at the first child element of a given {@link Dialog} and
     * removes it if it exists.
     *
     * Originally added to prevent a crash that occurs with nested PreferenceScreens that added
     * a toolbar via {@link WPActivityUtils#addToolbarToDialog(Fragment, Dialog, String)}. The
     * crash can be reproduced by turning 'Don't keep activities' on from Developer options.
     */
    public static void removeToolbarFromDialog(final Fragment context, final Dialog dialog) {
        if (dialog == null || !context.isAdded()) return;

        LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
        if (root.getChildAt(0) instanceof Toolbar) {
            root.removeViewAt(0);
        }
    }

    public static void setStatusBarColor(Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //noinspection deprecation
            window.setStatusBarColor(window.getContext().getResources().getColor(color));
        }
    }

    public static void hideKeyboard(final View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void applyLocale(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.contains(AppSettingsFragment.LANGUAGE_PREF_KEY)) {
            Locale contextLocale = context.getResources().getConfiguration().locale;
            String contextLanguage = contextLocale.getLanguage();
            contextLanguage = LanguageUtils.patchDeviceLanguageCode(contextLanguage);
            String contextCountry = contextLocale.getCountry();
            String locale = sharedPreferences.getString(AppSettingsFragment.LANGUAGE_PREF_KEY, "");

            if (!TextUtils.isEmpty(contextCountry)) {
                contextLanguage += "-" + contextCountry;
            }

            if (!locale.equals(contextLanguage)) {
                Resources resources = context.getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = new Locale(locale);
                resources.updateConfiguration(conf, resources.getDisplayMetrics());
            }
        }
    }

    public static Context getThemedContext(Context context) {
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity)context).getSupportActionBar();
            if (actionBar != null) {
                return actionBar.getThemedContext();
            }
        }
        return context;
    }

    public static boolean isEmailClientAvailable(Context context) {
        if (context == null) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> emailApps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return !emailApps.isEmpty();
    }
}
