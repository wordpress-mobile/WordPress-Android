package org.wordpress.android.util;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog.T;

import java.util.List;

public class WPActivityUtils {
    public static final String READER_DEEPLINK_ACTIVITY_ALIAS = "org.wordpress.android.WPComPostReaderActivity";

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

        if (dialogContainerView == null) {
            AppLog.e(T.SETTINGS, "Preference Dialog View was null when adding Toolbar");
            return;
        }

        // find the root view, then make sure the toolbar doesn't already exist
        ViewGroup root = (ViewGroup) dialogContainerView.getParent();

        // if we already added an appbar to the dialog it will be in the view one level above it's parent
        ViewGroup modifiedRoot = (ViewGroup) dialogContainerView.getParent().getParent();
        if (modifiedRoot != null && modifiedRoot.findViewById(R.id.appbar_main) != null) {
            return;
        }

        // remove view from the root
        root.removeView(dialogContainerView);

        // inflate our own layout with coordinator layout and appbar
        ViewGroup dialogViewWrapper = (ViewGroup) LayoutInflater.from(
                context.getActivity()).inflate(
                R.layout.preference_screen_wrapper, root,
                false);

        // container that will host content of the dialog
        ViewGroup listContainer = dialogViewWrapper.findViewById(R.id.list_container);

        final ListView listOfPreferences = dialogContainerView.findViewById(android.R.id.list);
        if (listOfPreferences != null) {
            ViewCompat.setNestedScrollingEnabled(listOfPreferences, true);
        }

        // add dialog view into container
        LayoutParams lp = dialogContainerView.getLayoutParams();
        lp.height = LayoutParams.MATCH_PARENT;
        lp.width = LayoutParams.MATCH_PARENT;
        listContainer.addView(dialogContainerView, lp);

        // add layout with container back to root view
        root.addView(dialogViewWrapper);

        AppBarLayout appbar = dialogViewWrapper.findViewById(R.id.appbar_main);
        MaterialToolbar toolbar = appbar.findViewById(R.id.toolbar_main);

        appbar.setLiftOnScrollTargetViewId(android.R.id.list);

        dialog.getWindow().setWindowAnimations(R.style.DialogAnimations);
        toolbar.setTitle(title);
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

        ViewGroup root = (ViewGroup) dialogContainerView.getParent().getParent();

        if (root.getChildAt(0) instanceof Toolbar) {
            root.removeViewAt(0);
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

    public static void disableReaderDeeplinks(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, READER_DEEPLINK_ACTIVITY_ALIAS),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    public static void enableReaderDeeplinks(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, READER_DEEPLINK_ACTIVITY_ALIAS),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
}
