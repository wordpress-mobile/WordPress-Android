package org.wordpress.android.util;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog.T;

import java.util.List;

public class WPActivityUtils {
    // Hack! PreferenceScreens don't show the toolbar, so we'll manually add one
    // See: http://stackoverflow.com/a/27455363/309558

    // TODO: android.app.Fragment  is deprecated since Android P.
    // Needs to be replaced with android.support.v4.app.Fragment
    // See https://developer.android.com/reference/android/app/Fragment
    public static void addToolbarToDialog(final android.app.Fragment context, final Dialog dialog, String title) {
        if (!context.isAdded() || dialog == null) {
            return;
        }

        View dialogContainerView = DialogExtensionsKt.getPreferenceDialogContainerView(dialog);

        // just in case, try to find a container of our own custom dialog
        if (dialogContainerView == null) {
            dialogContainerView = dialog.findViewById(R.id.list);
        }

        if (dialogContainerView == null) {
            AppLog.e(T.SETTINGS, "Preference Dialog View was null when adding Toolbar");
            return;
        }

        // find the root view, then make sure the toolbar doesn't already exist
        ViewGroup root = (ViewGroup) dialogContainerView.getParent();
        if (root.findViewById(R.id.toolbar) != null || root.findViewById(R.id.appbar_main) != null) {
            return;
        }

        AppBarLayout appbar = (AppBarLayout) LayoutInflater.from(context.getActivity())
                                                           .inflate(R.layout.toolbar_main, root, false);

        MaterialToolbar toolbar = appbar.findViewById(R.id.toolbar_main);

        root.addView(appbar, 0);

        dialog.getWindow().setWindowAnimations(R.style.DialogAnimations);


        toolbar.setTitle(title);
        toolbar.setNavigationIcon(org.wordpress.android.R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
        toolbar.setNavigationContentDescription(R.string.navigate_up_desc);
    }

    /**
     * Checks for a {@link Toolbar} at the first child element of a given {@link Dialog} and
     * removes it if it exists.
     * <p>
     * Originally added to prevent a crash that occurs with nested PreferenceScreens that added
     * a toolbar via {@link WPActivityUtils#addToolbarToDialog(android.app.Fragment, Dialog, String)}. The
     * crash can be reproduced by turning 'Don't keep activities' on from Developer options.
     */

    // TODO: android.app.Fragment  is deprecated since Android P.
    // Needs to be replaced with android.support.v4.app.Fragment
    // See https://developer.android.com/reference/android/app/Fragment
    public static void removeToolbarFromDialog(final android.app.Fragment context, final Dialog dialog) {
        if (dialog == null || !context.isAdded()) {
            return;
        }

        View dialogContainerView = DialogExtensionsKt.getPreferenceDialogContainerView(dialog);

        if (dialogContainerView == null) {
            AppLog.e(T.SETTINGS, "Preference Dialog View was null when removing Toolbar");
            return;
        }

        ViewGroup root = (ViewGroup) dialogContainerView.getParent();

        if (root.getChildAt(0) instanceof Toolbar) {
            root.removeViewAt(0);
        }
    }

    public static void setStatusBarColor(Window window, int color) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(window.getContext().getResources().getColor(color));
    }

    public static void setLightStatusBar(Window window, boolean showInLightMode) {
        Context context = window.getContext();
        boolean isDarkTheme = ConfigurationExtensionsKt.isDarkTheme(context.getResources().getConfiguration());
        if (!isDarkTheme) {
            int newColor = showInLightMode ? ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorSurface)
                    : ContextCompat.getColor(context, R.color.status_bar);
            window.setStatusBarColor(newColor);

            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                int systemVisibility = window.getDecorView().getSystemUiVisibility();
                int newSystemVisibility = showInLightMode ? systemVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                : systemVisibility ^ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                window.getDecorView().setSystemUiVisibility(newSystemVisibility);
            }
        }
    }

    public static Context getThemedContext(Context context) {
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
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

    public static void openEmailClient(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void disableComponent(Context context, Class<?> klass) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, klass),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    public static void enableComponent(Context context, Class<?> klass) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, klass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
}
