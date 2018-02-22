package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.wordpress.android.R;

/**
 * A {@link DialogFragment} implementing the full-screen dialog pattern defined in the
 * <a href="https://material.io/guidelines/components/dialogs.html#dialogs-full-screen-dialogs">
 * Material Design guidelines</a>.
 */
public class FullScreenDialogFragment extends DialogFragment {
    private Fragment mFragment;
    private FullScreenDialogController mController;
    private OnConfirmListener onConfirmListener;
    private OnDismissListener onDismissListener;
    private String mAction;
    private String mTitle;

    private static final String ARG_ACTION = "ARG_ACTION";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final int ID_ACTION = 1;

    public static final String TAG = FullScreenDialogFragment.class.getSimpleName();

    public interface FullScreenDialogContent {
        boolean onConfirmClicked(FullScreenDialogController controller);
        boolean onDismissClicked(FullScreenDialogController controller);
        void onViewCreated(FullScreenDialogController controller);
    }

    public interface FullScreenDialogController {
        void confirm(@Nullable Bundle result);
        void dismiss();
    }

    public interface OnConfirmListener {
        void onConfirm(@Nullable Bundle result);
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    protected static FullScreenDialogFragment newInstance(Builder builder) {
        FullScreenDialogFragment dialog = new FullScreenDialogFragment();
        dialog.setArguments(setArguments(builder));
        dialog.setContent(Fragment.instantiate(builder.mContext, builder.mClass.getName(), builder.mArguments));
        dialog.setOnConfirmListener(builder.mOnConfirmListener);
        dialog.setOnDismissListener(builder.mOnDismissListener);
        return dialog;
    }

    private static Bundle setArguments(Builder builder) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_ACTION, builder.mAction);
        bundle.putString(ARG_TITLE, builder.mTitle);
        return bundle;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.full_screen_dialog_fragment_none, 0, 0, R.anim.full_screen_dialog_fragment_none)
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

        mController = new FullScreenDialogController() {
            @Override
            public void confirm(@Nullable Bundle result) {
                FullScreenDialogFragment.this.confirm(result);
            }

            @Override
            public void dismiss() {
                FullScreenDialogFragment.this.dismiss();
            }
        };
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initBuilderArguments();

        Dialog dialog = new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                onDismissClicked();
            }
        };

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initBuilderArguments();

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.full_screen_dialog_fragment, container, false);
        initToolbar(view);
        setThemeBackground(view);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ((FullScreenDialogContent) getContent()).onViewCreated(mController);
    }

    @Override
    public void dismiss() {
        if (onDismissListener != null) {
            onDismissListener.onDismiss();
        }

        getFragmentManager().popBackStackImmediate();
    }

    @Override
    @SuppressLint("CommitTransaction")
    public void show(FragmentManager manager, String tag) {
        show(manager.beginTransaction(), tag);
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        initBuilderArguments();
        transaction.setCustomAnimations(R.anim.full_screen_dialog_fragment_slide_up, 0, 0, R.anim.full_screen_dialog_fragment_slide_down);
        return transaction.add(android.R.id.content, this, tag).addToBackStack(null).commit();
    }

    protected void confirm(Bundle result) {
        if (onConfirmListener != null) {
            onConfirmListener.onConfirm(result);
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
     * Initialize arguments passed in {@link Builder}.
     */
    private void initBuilderArguments() {
        Bundle bundle = getArguments();
        mTitle = bundle.getString(ARG_TITLE);
        mAction = bundle.getString(ARG_ACTION);
    }

    /**
     * Initialize toolbar title and action.
     *
     * @param view {@link View}
     */
    private void initToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.full_screen_dialog_fragment_toolbar);
        toolbar.setTitle(mTitle);
        toolbar.setNavigationIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_close_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDismissClicked();
            }
        });

        Menu menu = toolbar.getMenu();
        MenuItem action = menu.add(0, ID_ACTION, 0, this.mAction);
        action.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        action.setOnMenuItemClickListener(
            new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == ID_ACTION) {
                        onConfirmClicked();
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        );
    }

    public void onBackPressed() {
        if (isAdded()) {
            onDismissClicked();
        }
    }

    protected void onConfirmClicked() {
        boolean isConsumed = ((FullScreenDialogContent) mFragment).onConfirmClicked(mController);

        if (!isConsumed) {
            mController.confirm(null);
        }
    }

    protected void onDismissClicked() {
        boolean isConsumed = ((FullScreenDialogContent) mFragment).onDismissClicked(mController);

        if (!isConsumed) {
            mController.dismiss();
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
     * Set callback to call when dialog is closed due to confirm click.
     *
     * @param listener {@link OnConfirmListener} interface to call on confirm click
     */
    public void setOnConfirmListener(@Nullable OnConfirmListener listener) {
        this.onConfirmListener = listener;
    }

    /**
     * Set callback to call when dialog is closed due to dismiss click.
     *
     * @param listener {@link OnDismissListener} interface to call on dismiss click
     */
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    /**
     * Set theme background for {@link FullScreenDialogFragment} view.
     *
     * @param view {@link View} to set background
     */
    private void setThemeBackground(View view) {
        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            view.setBackgroundColor(value.data);
        } else {
            try {
                Drawable drawable = ResourcesCompat.getDrawable(getActivity().getResources(), value.resourceId, getActivity().getTheme());
                ViewCompat.setBackground(view, drawable);
            } catch (Resources.NotFoundException ignore) {
            }
        }
    }

    public static class Builder {
        Bundle mArguments;
        Class<? extends Fragment> mClass;
        Context mContext;
        OnConfirmListener mOnConfirmListener;
        OnDismissListener mOnDismissListener;
        String mAction;
        String mTitle;

        /**
         * Builder to construct {@link FullScreenDialogFragment}.
         *
         * @param context {@link Context}
         */
        public Builder(@NonNull Context context) {
            this.mContext = context;
        }

        /**
         * Creates {@link FullScreenDialogFragment} with provided parameters.
         *
         * @return {@link FullScreenDialogFragment} instance created
         */
        public FullScreenDialogFragment build() {
            return FullScreenDialogFragment.newInstance(this);
        }

        /**
         * Set {@link FullScreenDialogFragment} action text.
         *
         * @param text {@link String} to set as action text
         */
        public Builder setAction(@NonNull String text) {
            this.mAction = text;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} action text.
         *
         * @param textId resource ID to set as action text
         */
        public Builder setAction(@StringRes int textId) {
            return setAction(mContext.getString(textId));
        }

        /**
         * Set {@link Fragment} to be added as dialog, which must implement {@link FullScreenDialogContent}.
         *
         * @param contentClass     Fragment class to be instantiated
         * @param contentArguments arguments to be added to Fragment
         *
         * @return {@link Builder} object to allow for chaining of calls to set methods
         *
         * @throws IllegalArgumentException if content class does not implement {@link FullScreenDialogContent} interface
         */
        public Builder setContent(Class<? extends Fragment> contentClass, @Nullable Bundle contentArguments) {
            if (!FullScreenDialogContent.class.isAssignableFrom(contentClass)) {
                throw new IllegalArgumentException("The fragment class must implement FullScreenDialogContent interface");
            }

            this.mClass = contentClass;
            this.mArguments = contentArguments;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} title text.
         *
         * @param text {@link String} to set as title text
         */
        public Builder setTitle(@NonNull String text) {
            this.mTitle = text;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} title text.
         *
         * @param textId resource ID to set as title text
         */
        public Builder setTitle(@StringRes int textId) {
            this.mTitle = mContext.getString(textId);
            return this;
        }

        /**
         * Set callback to call when dialog is closed due to confirm click.
         *
         * @param listener {@link OnConfirmListener} interface to call on confirm click
         *
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setOnConfirmListener(@Nullable OnConfirmListener listener) {
            this.mOnConfirmListener = listener;
            return this;
        }

        /**
         * Set callback to call when dialog is closed due to dismiss click.
         *
         * @param listener {@link OnDismissListener} interface to call on dismiss click
         *
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setOnDismissListener(@Nullable OnDismissListener listener) {
            this.mOnDismissListener = listener;
            return this;
        }
    }
}
