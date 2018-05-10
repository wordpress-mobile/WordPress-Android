package org.wordpress.android.widgets;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.Snackbar.SnackbarLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * {@link Snackbar} with {@link android.app.Dialog}-like layout.  The layout views include title, message,
 * positive button, and negative button.  Any empty or null view is hidden.  The only required view is message.
 */
public class WPDialogSnackbar {
    private Snackbar mSnackbar;
    private View mContentView;

    private WPDialogSnackbar(@NonNull View view, @NonNull CharSequence text, int duration) {
        mSnackbar = Snackbar.make(view, "", duration);

        // Set underlying snackbar layout.
        SnackbarLayout snackbarLayout = (SnackbarLayout) mSnackbar.getView();
        snackbarLayout.setPadding(0, 0, 0, 0);

        // Hide underlying snackbar text and action.
        TextView snackbarText = snackbarLayout.findViewById(android.support.design.R.id.snackbar_text);
        snackbarText.setVisibility(View.INVISIBLE);
        TextView snackbarAction = snackbarLayout.findViewById(android.support.design.R.id.snackbar_action);
        snackbarAction.setVisibility(View.INVISIBLE);

        mContentView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_snackbar, null);

        TextView message = mContentView.findViewById(R.id.message);

        // Hide message view when text is empty.
        if (TextUtils.isEmpty(text)) {
            message.setVisibility(View.GONE);
        } else {
            message.setVisibility(View.VISIBLE);
            message.setText(text);
        }

        snackbarLayout.addView(mContentView, 0);
    }

    public void dismiss() {
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
    }

    public boolean isShowing() {
        return mSnackbar != null && mSnackbar.isShown();
    }

    public static WPDialogSnackbar make(@NonNull View view, @NonNull CharSequence text, int duration) {
        return new WPDialogSnackbar(view, text, duration);
    }

    private void setButtonTextAndVisibility(TextView button, CharSequence text, final View.OnClickListener listener) {
        // Hide button when text is empty or listener is null.
        if (TextUtils.isEmpty(text) || listener == null) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
        } else {
            button.setVisibility(View.VISIBLE);
            button.setText(text);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClick(view);
                    dismiss();
                }
            });
        }
    }

    public WPDialogSnackbar setNegativeButton(CharSequence text, View.OnClickListener listener) {
        setButtonTextAndVisibility((TextView) mContentView.findViewById(R.id.button_negative), text, listener);
        return this;
    }

    public WPDialogSnackbar setNeutralButton(CharSequence text, View.OnClickListener listener) {
        setButtonTextAndVisibility((TextView) mContentView.findViewById(R.id.button_neutral), text, listener);
        return this;
    }

    public WPDialogSnackbar setPositiveButton(CharSequence text, View.OnClickListener listener) {
        setButtonTextAndVisibility((TextView) mContentView.findViewById(R.id.button_positive), text, listener);
        return this;
    }

    public WPDialogSnackbar setTitle(@NonNull CharSequence text) {
        TextView title = mContentView.findViewById(R.id.title);

        // Hide title view when text is empty.
        if (TextUtils.isEmpty(text)) {
            title.setVisibility(View.GONE);
        } else {
            title.setVisibility(View.VISIBLE);
            title.setText(text);
        }

        return this;
    }

    public void show() {
        mSnackbar.show();
    }
}
