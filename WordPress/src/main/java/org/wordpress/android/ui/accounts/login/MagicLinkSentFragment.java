package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.util.HelpshiftHelper;

public class MagicLinkSentFragment extends Fragment {
    public interface OnMagicLinkSentInteraction {
        void onEnterPasswordRequested();
    }

    private OnMagicLinkSentInteraction mListener;

    public MagicLinkSentFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMagicLinkSentInteraction) {
            mListener = (OnMagicLinkSentInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnMagicLinkSentInteraction");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.magic_link_sent_fragment, container, false);

        TextView enterPasswordView = (TextView) view.findViewById(R.id.password_layout);
        enterPasswordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onEnterPasswordRequested();
                }
            }
        });

        TextView openEmailView = (TextView) view.findViewById(R.id.open_email_button);
        openEmailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmailClient();
            }
        });
        
        initInfoButtons(view);

        return view;
    }

    private void initInfoButtons(View rootView) {
        View.OnClickListener infoButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), HelpActivity.class);
                intent.putExtra(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.Tag.ORIGIN_LOGIN_SCREEN_HELP);
                startActivity(intent);
            }
        };
        ImageView infoButton = (ImageView) rootView.findViewById(R.id.info_button);
        infoButton.setOnClickListener(infoButtonListener);
    }

    private void openEmailClient() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        getActivity().startActivity(intent);
    }
}
