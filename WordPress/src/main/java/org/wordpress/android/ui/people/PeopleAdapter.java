package org.wordpress.android.ui.people;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class PeopleAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private List<Person> mPeopleList;
    private int mAvatarSz;

    public PeopleAdapter(Context context, List<Person> peopleList) {
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.people_avatar_sz);
        mInflater = LayoutInflater.from(context);
        mPeopleList = peopleList;
    }

    public void setPeopleList(List<Person> peopleList) {
        mPeopleList = peopleList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mPeopleList == null) {
            return 0;
        }
        return mPeopleList.size();
    }

    @Override
    public Person getItem(int position) {
        if (mPeopleList == null) {
            return null;
        }
        return mPeopleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        Person person = getItem(position);
        if (person == null) {
            return -1;
        }
        return person.getPersonID();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PeopleViewHolder holder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.people_list_row, parent, false);
            holder = new PeopleViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (PeopleViewHolder) convertView.getTag();
        }

        Person person = getItem(position);

        if (person != null) {
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), mAvatarSz);
            holder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            holder.txtDisplayName.setText(person.getDisplayName());
            holder.txtUsername.setText(String.format("@%s", person.getUsername()));
            holder.txtRole.setText(StringUtils.capitalize(person.getRole()));
        }

        return convertView;
    }

    private class PeopleViewHolder {
        private final WPNetworkImageView imgAvatar;
        private final TextView txtDisplayName;
        private final TextView txtUsername;
        private final TextView txtRole;

        PeopleViewHolder(View row) {
            imgAvatar = (WPNetworkImageView) row.findViewById(R.id.person_avatar);
            txtDisplayName = (TextView) row.findViewById(R.id.person_display_name);
            txtUsername = (TextView) row.findViewById(R.id.person_username);
            txtRole = (TextView) row.findViewById(R.id.person_role);
        }
    }
}