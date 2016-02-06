package org.wordpress.android.util;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.SettingsFragment;

import java.util.Locale;

public class WPActivityUtils {
    // Hack! PreferenceScreens don't show the toolbar, so we'll manually add one
    // See: http://stackoverflow.com/a/27455363/309558
    public static void addToolbarToDialog(final Fragment context, final Dialog dialog, String title) {
        if (!context.isAdded() || dialog == null) {
            return;
        }

        Toolbar toolbar;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (dialog.findViewById(android.R.id.list) == null) {
                return;
            }

            LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
            toolbar = (Toolbar) LayoutInflater.from(context.getActivity()).inflate(org.wordpress.android.R.layout.toolbar, root, false);
            root.addView(toolbar, 0);
        } else {
            if (dialog.findViewById(android.R.id.content) == null) {
                return;
            }

            ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.content);
            if (!(root.getChildAt(0) instanceof ListView)) {
                return;
            }

            ListView content = (ListView) root.getChildAt(0);
            root.removeAllViews();

            toolbar = (Toolbar) LayoutInflater.from(context.getActivity()).inflate(org.wordpress.android.R.layout.toolbar, root, false);
            int height;
            TypedValue tv = new TypedValue();
            if (context.getActivity().getTheme().resolveAttribute(org.wordpress.android.R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            } else{
                height = toolbar.getHeight();
            }

            content.setPadding(0, height, 0, 0);
            root.addView(content);
            root.addView(toolbar);
        }

        dialog.getWindow().setWindowAnimations(R.style.DialogAnimations);

        TextView titleView = (TextView) toolbar.findViewById(R.id.toolbar_title);
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(title);

        toolbar.setTitle("");
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setNavigationIcon(org.wordpress.android.R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
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

    public static void applyLocale(Activity context, boolean restart) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.contains(SettingsFragment.LANGUAGE_PREF_KEY)) {
            Locale contextLocale = context.getResources().getConfiguration().locale;
            String contextLanguage = contextLocale.getLanguage();
            String contextCountry = contextLocale.getCountry();
            String locale = sharedPreferences.getString(SettingsFragment.LANGUAGE_PREF_KEY, "");

            if (!TextUtils.isEmpty(contextCountry)) {
                contextLanguage += "-" + contextCountry;
            }

            if (!locale.equals(contextLanguage)) {
                Resources resources = context.getResources();
                Configuration conf = resources.getConfiguration();
                conf.locale = new Locale(locale);
                resources.updateConfiguration(conf, resources.getDisplayMetrics());

                if (restart) {
                    Intent refresh = new Intent(context, context.getClass());
                    refresh.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    context.startActivity(refresh);
                    context.finish();
                    context.overridePendingTransition(0, 0);
                }
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
}
