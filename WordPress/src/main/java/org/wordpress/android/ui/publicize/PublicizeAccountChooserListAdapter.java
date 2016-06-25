package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by Will on 6/24/16.
 */
public class PublicizeAccountChooserListAdapter extends ArrayAdapter {

    public PublicizeAccountChooserListAdapter(Context context, int resource, int resource2, PublicizeEvents.Connection[] objects) {
        super(context, resource, resource2, objects);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.publicize_connection_list_item, viewGroup, false);
        PublicizeEvents.Connection connection = (PublicizeEvents.Connection)getItem(i);
        WPNetworkImageView imageView = (WPNetworkImageView) rowView.findViewById(R.id.profile_pic);
        imageView.setImageUrl(connection.getProfilePictureUrl().toString(), WPNetworkImageView.ImageType.AVATAR);

        TextView name = (TextView) rowView.findViewById(R.id.name);
        name.setText(connection.getDisplayName());

        return rowView;
    }
}
