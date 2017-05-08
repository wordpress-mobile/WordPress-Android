package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.wordpress.android.R;

public class PostExcerptDialogFragment extends DialogFragment {
    interface PostExcerptDialogListener {
        void onPostExcerptUpdated(String postExcerpt);
    }

    private static final String EXCERPT_TAG = "excerpt";
    private String mPostExcerpt;
    private PostExcerptDialogListener mPostExcerptDialogListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPostExcerpt = savedInstanceState.getString(EXCERPT_TAG, "");
        } else if (getArguments() != null) {
            mPostExcerpt = getArguments().getString(EXCERPT_TAG, "");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXCERPT_TAG, mPostExcerpt);
    }

    public static PostExcerptDialogFragment newInstance(String postExcerpt) {
        PostExcerptDialogFragment dialogFragment = new PostExcerptDialogFragment();
        Bundle args = new Bundle();
        args.putString(EXCERPT_TAG, postExcerpt);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View dialogView = layoutInflater.inflate(R.layout.post_excerpt_dialog, null);
        builder.setView(dialogView);
        final EditText editText = (EditText) dialogView.findViewById(R.id.post_excerpt_dialog_edit_text);
        if (!TextUtils.isEmpty(mPostExcerpt)) {
            editText.setText(mPostExcerpt);
            // move the cursor to the end
            editText.setSelection(mPostExcerpt.length());
        }

        builder.setTitle(R.string.post_excerpt);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPostExcerpt = editText.getText().toString();
                if (mPostExcerptDialogListener != null) {
                    mPostExcerptDialogListener.onPostExcerptUpdated(mPostExcerpt);
                }
            }
        });

        return builder.create();
    }

    public void setPostExcerptDialogListener(PostExcerptDialogListener postExcerptDialogListener) {
        mPostExcerptDialogListener = postExcerptDialogListener;
    }
}
