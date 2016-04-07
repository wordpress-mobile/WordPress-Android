package org.wordpress.android.ui.people;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PersonDetailFragment extends Fragment {

    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private TextView mRoleTextView;
    private TextView mRemoveTextView;

    public static PersonDetailFragment newInstance() {
        return new PersonDetailFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.person_detail_fragment, container, false);

        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.person_avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.person_display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.person_username);
        mRoleTextView = (TextView) rootView.findViewById(R.id.person_role);
        mRemoveTextView = (TextView) rootView.findViewById(R.id.person_remove);

        return rootView;
    }

    public void setPerson(Person person) {
        if (person != null) {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), avatarSz);

            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            mDisplayNameTextView.setText(person.getDisplayName());
            mUsernameTextView.setText(person.getUsername());
            mRoleTextView.setText(Role.getLabel(getActivity(), person.getRole()));
            mRemoveTextView.setText(String.format(getString(R.string.remove_user), person.getFirstName().toUpperCase()));

            mRemoveTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO: remove user
                }
            });
        }
    }
}
