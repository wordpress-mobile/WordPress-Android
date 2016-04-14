package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;

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
        View view = inflater.inflate(R.layout.fragment_magic_link_sent, container, false);

        TextView enterPasswordView = (TextView) view.findViewById(R.id.password_layout);
        enterPasswordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onEnterPasswordRequested();
                }
            }
        });

        return view;
    }
}
