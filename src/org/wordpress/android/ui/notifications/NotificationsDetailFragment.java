package org.wordpress.android.ui.notifications;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;
import org.wordpress.android.widgets.NoticonTextView;
import org.wordpress.android.widgets.WPTextView;

/**
 * Detail fragment for notifications. Displays the note subject and expects a
 * Sub Fragment to be passed for display beneath the header.
 *
 */
public class NotificationsDetailFragment extends Fragment implements NotificationFragment {
    public static int NOTIFICATIONS_TRANSIT_ID = 1;
    private Note mNote;
    private Fragment mSubFragment;
    private static float mYPosition;

    public NotificationsDetailFragment() {
        // Required empty public constructor
    }

    public static NotificationsDetailFragment newInstance(final Note note, float yPosition) {
        NotificationsDetailFragment fragment = new NotificationsDetailFragment();
        fragment.setNote(note);
        mYPosition = yPosition;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.notifications_detail_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            if (mNote.getSubject() != null) {
                NoticonTextView noticonTextView = (NoticonTextView) view.findViewById(R.id.notification_header_icon);
                noticonTextView.setText(mNote.getNoticonCharacter());

                WPTextView subjectTextView = (WPTextView) view.findViewById(R.id.notification_header_subject);
                subjectTextView.setText(mNote.getSubject());

            }

            if (mSubFragment != null) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                fragmentTransaction.replace(R.id.notification_detail_fragment_container, mSubFragment);
                fragmentTransaction.commitAllowingStateLoss();
            }
        }
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    @Override
    public void setNote(Note note) {
        mNote = note;
    }


    public void setFragment(Fragment fragment) {
        mSubFragment = fragment;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (transit != NOTIFICATIONS_TRANSIT_ID || !enter) {
            return null;
        }

        ObjectAnimator enterAnimation = ObjectAnimator.ofFloat(null, "translationY", mYPosition, 0.0f);
        enterAnimation.setDuration(NewNotificationsActivity.NOTIFICATION_TRANSITION_DURATION);

        return enterAnimation;
    }
}
