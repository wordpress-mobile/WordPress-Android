package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.wordpress.android.R;

public class PostExcerptDialogFragment extends DialogFragment {
    private static final String EXCERPT_TAG = "excerpt";
    private String mPostExcerpt;

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
        if (savedInstanceState != null) {
            mPostExcerpt = savedInstanceState.getString(EXCERPT_TAG, "");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        LayoutInflater factory = LayoutInflater.from(getActivity());
        View textEntryView = factory.inflate(R.layout.post_excerpt_dialog, null);
        builder.setView(textEntryView);
        builder.setTitle(R.string.post_excerpt);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        EditText editText = (EditText) textEntryView.findViewById(R.id.post_excerpt_dialog_edit_text);
        editText.setText(mPostExcerpt);
        return dialog;
    }
}
