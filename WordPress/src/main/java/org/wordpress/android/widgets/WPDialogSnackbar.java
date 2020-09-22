package org.wordpress.android.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.Snackbar.SnackbarLayout;

import org.wordpress.android.R;
import org.wordpress.android.util.AccessibilityUtils;

/**
 * {@link Snackbar} with {@link android.app.Dialog}-like layout mimicking the updated design pattern defined in the
 * Material Design guidelines <a href="https://material.io/design/components/snackbars.html#spec">specifications</a>.
 * The view include title, message, positive button, negative button, and neutral button.  Any empty or null view is
 * hidden.  The only required view is message.
 */
public class WPDialogSnackbar {
    private Snackbar mSnackbar;
    private View mContentView;

    private WPDialogSnackbar(@NonNull View view, @NonNull CharSequence text, int duration) {
        mSnackbar = Snackbar.make(view, "", // CHECKSTYLE IGNORE
                AccessibilityUtils.getSnackbarDuration(view.getContext(), duration));

        // Set underlying snackbar layout.
        SnackbarLayout snackbarLayout = (SnackbarLayout) mSnackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarLayout.getLayoutParams();
        Context context = view.getContext();
        int margin = (int) context.getResources().getDimension(R.dimen.margin_medium);
        params.setMargins(margin, margin, margin, margin);
        snackbarLayout.setLayoutParams(params);
        snackbarLayout.setPadding(0, 0, 0, 0);
        snackbarLayout.setBackgroundResource(R.drawable.bg_snackbar);

        // Hide underlying snackbar text and action.
        TextView snackbarText = snackbarLayout.findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarText.setVisibility(View.INVISIBLE);
        TextView snackbarAction = snackbarLayout.findViewById(com.google.android.material.R.id.snackbar_action);
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

    private void setButtonTextAndVisibility(Button button, CharSequence text, final View.OnClickListener listener) {
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
        setButtonTextAndVisibility((Button) mContentView.findViewById(R.id.button_negative), text, listener);
        return this;
    }

    public WPDialogSnackbar setNeutralButton(CharSequence text, View.OnClickListener listener) {
        setButtonTextAndVisibility((Button) mContentView.findViewById(R.id.button_neutral), text, listener);
        return this;
    }

    public WPDialogSnackbar setPositiveButton(CharSequence text, View.OnClickListener listener) {
        setButtonTextAndVisibility((Button) mContentView.findViewById(R.id.button_positive), text, listener);
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
