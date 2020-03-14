package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.elevation.ElevationOverlayProvider;

import org.wordpress.android.R;

/**
 * A {@link DialogFragment} implementing the full-screen dialog pattern defined in the
 * <a href="https://material.io/guidelines/components/dialogs.html#dialogs-full-screen-dialogs">
 * Material Design guidelines</a> with an icon rather than text action.
 */
public class CollapseFullScreenDialogFragment extends DialogFragment {
    private Fragment mFragment;
    private CollapseFullScreenDialogController mController;
    private MenuItem mMenuAction;
    private OnConfirmListener mOnConfirmListener;
    private OnCollapseListener mOnCollapseListener;
    private String mAction;
    private String mTitle;
    private boolean mHideActivityBar;

    private static final String ARG_ACTION = "ARG_ACTION";
    private static final String ARG_HIDE_ACTIVITY_BAR = "ARG_HIDE_ACTIVITY_BAR";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final int ID_ACTION = 1;

    public static final String TAG = CollapseFullScreenDialogFragment.class.getSimpleName();

    public interface CollapseFullScreenDialogContent {
        boolean onCollapseClicked(CollapseFullScreenDialogController controller);

        boolean onConfirmClicked(CollapseFullScreenDialogController controller);

        void onViewCreated(CollapseFullScreenDialogController controller);
    }

    public interface CollapseFullScreenDialogController {
        void collapse(@Nullable Bundle result);

        void confirm(@Nullable Bundle result);

        void setConfirmEnabled(boolean enabled);
    }

    public interface OnCollapseListener {
        void onCollapse(@Nullable Bundle result);
    }

    public interface OnConfirmListener {
        void onConfirm(@Nullable Bundle result);
    }

    protected static CollapseFullScreenDialogFragment newInstance(Builder builder) {
        CollapseFullScreenDialogFragment dialog = new CollapseFullScreenDialogFragment();
        dialog.setArguments(setArguments(builder));
        dialog.setContent(Fragment.instantiate(builder.mContext, builder.mClass.getName(), builder.mArguments));
        dialog.setOnCollapseListener(builder.mOnCollapseListener);
        dialog.setOnConfirmListener(builder.mOnConfirmListener);
        dialog.setHideActivityBar(builder.mHideActivityBar);
        return dialog;
    }

    private static Bundle setArguments(Builder builder) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_ACTION, builder.mAction);
        bundle.putString(ARG_TITLE, builder.mTitle);
        bundle.putBoolean(ARG_HIDE_ACTIVITY_BAR, builder.mHideActivityBar);
        return bundle;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.full_screen_dialog_fragment_none, 0, 0,
                            R.anim.full_screen_dialog_fragment_none)
                    .add(R.id.full_screen_dialog_fragment_content, mFragment)
                    .commitNow();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mFragment = getChildFragmentManager().findFragmentById(R.id.full_screen_dialog_fragment_content);
        }

        mController = new CollapseFullScreenDialogController() {
            @Override
            public void collapse(@Nullable Bundle result) {
                CollapseFullScreenDialogFragment.this.collapse(result);
            }

            @Override
            public void confirm(@Nullable Bundle result) {
                CollapseFullScreenDialogFragment.this.confirm(result);
            }

            @Override
            public void setConfirmEnabled(boolean enabled) {
                if (CollapseFullScreenDialogFragment.this.mMenuAction != null) {
                    CollapseFullScreenDialogFragment.this.mMenuAction.setEnabled(enabled);
                }
            }
        };
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initBuilderArguments();

        Dialog dialog = new Dialog(requireContext(), getTheme()) {
            @Override
            public void onBackPressed() {
                onCollapseClicked();
            }
        };

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        initBuilderArguments();

        if (mHideActivityBar) {
            hideActivityBar();
        }

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.collapse_full_screen_dialog_fragment, container, false);
        initToolbar(view);
        setThemeBackground(view);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((CollapseFullScreenDialogContent) getContent()).onViewCreated(mController);
    }

    @Override
    public void dismiss() {
        if (mHideActivityBar) {
            showActivityBar();
        }

        if (getFragmentManager() != null) {
            getFragmentManager().popBackStackImmediate();
        }
    }

    @Override
    @SuppressLint("CommitTransaction")
    public void show(FragmentManager manager, String tag) {
        show(manager.beginTransaction(), tag);
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        initBuilderArguments();
        transaction.setCustomAnimations(R.anim.full_screen_dialog_fragment_slide_up, 0, 0,
                R.anim.full_screen_dialog_fragment_slide_down);
        return transaction.add(android.R.id.content, this, tag).addToBackStack(null).commit();
    }

    protected void collapse(Bundle result) {
        if (mOnCollapseListener != null) {
            mOnCollapseListener.onCollapse(result);
        }

        dismiss();
    }

    protected void confirm(Bundle result) {
        if (mOnConfirmListener != null) {
            mOnConfirmListener.onConfirm(result);
        }

        dismiss();
    }

    /**
     * Get {@link Fragment} to be able to interact directly with it.
     *
     * @return {@link Fragment} dialog content
     */
    public Fragment getContent() {
        return this.mFragment;
    }

    /**
     * Hide {@link AppCompatActivity} bar when showing fullscreen dialog.
     */
    public void hideActivityBar() {
        FragmentActivity activity = getActivity();

        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();

            if (actionBar != null && actionBar.isShowing()) {
                actionBar.hide();
            }
        }
    }

    /**
     * Initialize arguments passed in {@link Builder}.
     */
    private void initBuilderArguments() {
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            mAction = bundle.getString(ARG_ACTION);
            mTitle = bundle.getString(ARG_TITLE);
            mHideActivityBar = bundle.getBoolean(ARG_HIDE_ACTIVITY_BAR);
        }
    }

    /**
     * Initialize toolbar title and action.
     *
     * @param view {@link View}
     */
    private void initToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.full_screen_dialog_fragment_toolbar);

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(view.getContext());
        float appbarElevation = getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(appbarElevation);
        toolbar.setBackgroundColor(elevatedColor);

        toolbar.setTitle(mTitle);
        toolbar.setNavigationContentDescription(R.string.description_collapse);
        toolbar.setNavigationIcon(ContextCompat.getDrawable(view.getContext(), R.drawable.ic_chevron_down_white_24dp));
        toolbar.setNavigationOnClickListener(v -> onCollapseClicked());

        if (!mAction.isEmpty()) {
            Menu menu = toolbar.getMenu();
            mMenuAction = menu.add(0, ID_ACTION, 0, this.mAction);
            mMenuAction.setIcon(R.drawable.ic_send_white_24dp);
            MenuItemCompat.setIconTintList(mMenuAction,
                    AppCompatResources.getColorStateList(view.getContext(), R.color.accent_neutral_30_selector));
            mMenuAction.setEnabled(false);
            mMenuAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            mMenuAction.setOnMenuItemClickListener(
                    item -> {
                        if (item.getItemId() == ID_ACTION) {
                            onConfirmClicked();
                            return true;
                        } else {
                            return false;
                        }
                    }
            );
        }
    }

    public void onBackPressed() {
        if (isAdded()) {
            onCollapseClicked();
        }
    }

    protected void onConfirmClicked() {
        boolean isConsumed = ((CollapseFullScreenDialogContent) mFragment).onConfirmClicked(mController);

        if (!isConsumed) {
            mController.confirm(null);
        }
    }

    protected void onCollapseClicked() {
        boolean isConsumed = ((CollapseFullScreenDialogContent) mFragment).onCollapseClicked(mController);

        if (!isConsumed) {
            mController.collapse(null);
        }
    }

    /**
     * Set {@link Fragment} as dialog content.
     *
     * @param fragment {@link Fragment} to set as dialog content
     */
    private void setContent(Fragment fragment) {
        this.mFragment = fragment;
    }

    /**
     * Set flag to hide activity bar when showing fullscreen dialog.
     *
     * @param hide boolean to hide activity bar
     */
    public void setHideActivityBar(boolean hide) {
        this.mHideActivityBar = hide;
    }

    /**
     * Set callback to call when dialog is closed due to collapse click.
     *
     * @param listener {@link OnCollapseListener} interface to call on collapse click
     */
    public void setOnCollapseListener(@Nullable OnCollapseListener listener) {
        this.mOnCollapseListener = listener;
    }

    /**
     * Set callback to call when dialog is closed due to confirm click.
     *
     * @param listener {@link OnConfirmListener} interface to call on confirm click
     */
    public void setOnConfirmListener(@Nullable OnConfirmListener listener) {
        this.mOnConfirmListener = listener;
    }

    /**
     * Set theme background for {@link CollapseFullScreenDialogFragment} view.
     *
     * @param view {@link View} to set background
     */
    private void setThemeBackground(View view) {
        TypedValue value = new TypedValue();
        requireActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            view.setBackgroundColor(value.data);
        } else {
            try {
                Drawable drawable = ResourcesCompat.getDrawable(requireActivity().getResources(), value.resourceId,
                        requireActivity().getTheme());
                ViewCompat.setBackground(view, drawable);
            } catch (Resources.NotFoundException ignore) {
            }
        }
    }

    /**
     * Show {@link AppCompatActivity} bar when hiding fullscreen dialog.
     */
    public void showActivityBar() {
        FragmentActivity activity = getActivity();

        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();

            if (actionBar != null && !actionBar.isShowing()) {
                actionBar.show();
            }
        }
    }

    public static class Builder {
        Bundle mArguments;
        Class<? extends Fragment> mClass;
        Context mContext;
        OnCollapseListener mOnCollapseListener;
        OnConfirmListener mOnConfirmListener;
        String mAction = "";
        String mTitle = "";
        boolean mHideActivityBar = false;

        /**
         * Builder to construct {@link CollapseFullScreenDialogFragment}.
         *
         * @param context {@link Context}
         */
        public Builder(@NonNull Context context) {
            this.mContext = context;
        }

        /**
         * Creates {@link CollapseFullScreenDialogFragment} with provided parameters.
         *
         * @return {@link CollapseFullScreenDialogFragment} instance created
         */
        public CollapseFullScreenDialogFragment build() {
            return CollapseFullScreenDialogFragment.newInstance(this);
        }

        /**
         * Set {@link CollapseFullScreenDialogFragment} action text.
         *
         * @param text {@link String} to set as action text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setAction(@NonNull String text) {
            this.mAction = text;
            return this;
        }

        /**
         * Set {@link CollapseFullScreenDialogFragment} action text.
         *
         * @param textId resource ID to set as action text
         */
        public Builder setAction(@StringRes int textId) {
            return setAction(mContext.getString(textId));
        }

        /**
         * Set {@link Fragment} to be added as dialog, which must implement {@link CollapseFullScreenDialogContent}.
         *
         * @param contentClass     Fragment class to be instantiated
         * @param contentArguments arguments to be added to Fragment
         * @return {@link Builder} object to allow for chaining of calls to set methods
         * @throws IllegalArgumentException if content class does not implement
         *                                  {@link CollapseFullScreenDialogContent} interface
         */
        public Builder setContent(Class<? extends Fragment> contentClass, @Nullable Bundle contentArguments) {
            if (!CollapseFullScreenDialogContent.class.isAssignableFrom(contentClass)) {
                throw new IllegalArgumentException(
                        "The fragment class must implement CollapseFullScreenDialogContent interface");
            }

            this.mClass = contentClass;
            this.mArguments = contentArguments;
            return this;
        }

        /**
         * Set flag to hide activity bar when showing fullscreen dialog.
         *
         * @param hide boolean to hide activity bar
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setHideActivityBar(boolean hide) {
            this.mHideActivityBar = hide;
            return this;
        }

        /**
         * Set callback to call when dialog is closed due to collapse click.
         *
         * @param listener {@link OnCollapseListener} interface to call on collapse click
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setOnCollapseListener(@Nullable OnCollapseListener listener) {
            this.mOnCollapseListener = listener;
            return this;
        }

        /**
         * Set callback to call when dialog is closed due to confirm click.
         *
         * @param listener {@link OnConfirmListener} interface to call on confirm click
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setOnConfirmListener(@Nullable OnConfirmListener listener) {
            this.mOnConfirmListener = listener;
            return this;
        }

        /**
         * Set {@link CollapseFullScreenDialogFragment} title text.
         *
         * @param text {@link String} to set as title text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setTitle(@NonNull String text) {
            this.mTitle = text;
            return this;
        }

        /**
         * Set {@link CollapseFullScreenDialogFragment} title text.
         *
         * @param textId resource ID to set as title text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setTitle(@StringRes int textId) {
            this.mTitle = mContext.getString(textId);
            return this;
        }
    }
}
