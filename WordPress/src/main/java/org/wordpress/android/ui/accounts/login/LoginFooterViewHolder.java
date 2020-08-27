package org.wordpress.android.ui.accounts.login;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;

public class LoginFooterViewHolder extends RecyclerView.ViewHolder {
    private final TextView mFooterTextView;

    LoginFooterViewHolder(View view) {
        super(view);
        mFooterTextView = view.findViewById(R.id.footer_text_view);
    }

    public void bindText(String text) {
        mFooterTextView.setText(text);
    }
}
