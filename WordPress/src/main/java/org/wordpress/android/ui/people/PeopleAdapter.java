package org.wordpress.android.ui.people;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;

public class PeopleAdapter extends BaseAdapter {
    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<Person> mPersonList;
    private int mAvatarSz;

    public PeopleAdapter(Context context) {
        mContext = context;
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
        mInflater = LayoutInflater.from(context);
        mPersonList = new ArrayList<>();
        mPersonList.add(new Person(1, "beaulebens", "Beau", "Lebens", "Beau", "http://lorempixum.com/76/76", Role.ADMIN));
        mPersonList.add(new Person(2, "ebinnion", "Eric", "Binnion", "Eric", "http://lorempixum.com/76/76", Role.AUTHOR));
        mPersonList.add(new Person(3, "javialvarez", "Javi", "Alvarez", "Javi", "http://lorempixum.com/76/76", Role.CONTRIBUTOR));
        mPersonList.add(new Person(4, "oguzkocer", "Oguz", "Kocer", "Oguz", "http://lorempixum.com/76/76", Role.EDITOR));
    }

    @Override
    public int getCount() {
        if (mPersonList == null) {
            return 0;
        }
        return mPersonList.size();
    }

    @Override
    public Person getItem(int position) {
        if (mPersonList == null) {
            return null;
        }
        return mPersonList.get(position);
    }

    @Override
    public long getItemId(int position) {
        Person person = getItem(position);
        if (person == null) {
            return 0;
        }
        return person.personID;
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
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getImageUrl(), mAvatarSz);
            holder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            holder.txtDisplayName.setText(person.getDisplayName());
            holder.txtUsername.setText(person.getUsername());
            holder.txtRole.setText(Role.toString(mContext, person.getRole()));
            holder.txtRole.setBackgroundColor(Role.backgroundColor(mContext, person.getRole()));
        }

        // hide the divider for the last item
        boolean isLastItem = (position == getCount() - 1);
        holder.divider.setVisibility(isLastItem ?  View.INVISIBLE : View.VISIBLE);

        return convertView;
    }

    private class PeopleViewHolder {
        private final WPNetworkImageView imgAvatar;
        private final TextView txtDisplayName;
        private final TextView txtUsername;
        private final TextView txtRole;
        private final View divider;

        PeopleViewHolder(View row) {
            imgAvatar = (WPNetworkImageView) row.findViewById(R.id.people_list_row_avatar);
            txtDisplayName = (TextView) row.findViewById(R.id.people_list_row_display_name);
            txtUsername = (TextView) row.findViewById(R.id.people_list_row_username);
            txtRole = (TextView) row.findViewById(R.id.people_list_row_role);
            divider = row.findViewById(R.id.divider);
        }
    }
}