package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockDialogFragment;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

public class NUXDialogFragment extends SherlockDialogFragment {

    private static String ARG_TITLE = "title";
    private static String ARG_DESCRIPTION = "message";
    private static String ARG_FOOTER = "footer";
    private static String ARG_IMAGE = "image";

    private ImageView mImageView;
    private WPTextView mTitleTextView;
    private WPTextView mDescriptionTextView;
    private WPTextView mFooterTextView;

    public NUXDialogFragment() {
        // Empty constructor required for DialogFragment
    }


    public static NUXDialogFragment newInstance(String title, String message, String footer, int imageSource) {
        NUXDialogFragment adf = new NUXDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_TITLE, title);
        bundle.putString(ARG_DESCRIPTION, message);
        bundle.putString(ARG_FOOTER, footer);
        bundle.putInt(ARG_IMAGE, imageSource);
        adf.setArguments(bundle);
        adf.setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme);
        return adf;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(getResources().getDrawable(R.color.nux_alert_bg));
        View v = inflater.inflate(R.layout.nux_dialog_fragment, container, false);

        mImageView = (ImageView)v.findViewById(R.id.nux_dialog_image);
        mTitleTextView = (WPTextView)v.findViewById(R.id.nux_dialog_title);
        mDescriptionTextView = (WPTextView)v.findViewById(R.id.nux_dialog_description);
        mFooterTextView = (WPTextView)v.findViewById(R.id.nux_dialog_footer);

        Bundle args = this.getArguments();

        mTitleTextView.setText(args.getString(ARG_TITLE));
        mDescriptionTextView.setText(args.getString(ARG_DESCRIPTION));
        mFooterTextView.setText(args.getString(ARG_FOOTER));
        mImageView.setImageResource(args.getInt(ARG_IMAGE));

        v.setClickable(true);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        return v;
    }
}

