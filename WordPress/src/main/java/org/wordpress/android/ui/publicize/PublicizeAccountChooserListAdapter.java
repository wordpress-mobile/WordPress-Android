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

        super(context, resource, resource2, objects);
    public PublicizeAccountChooserListAdapter(Context context, int resource, PublicizeConnection[] objects, boolean isConnected) {
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.publicize_connection_list_item, viewGroup, false);
        PublicizeConnection publicizeConnection = (PublicizeConnection)getItem(i);
        WPNetworkImageView imageView = (WPNetworkImageView) rowView.findViewById(R.id.profile_pic);
        imageView.setImageUrl(publicizeConnection.getExternalProfilePictureUrl().toString(), WPNetworkImageView.ImageType.PHOTO);

        TextView name = (TextView) rowView.findViewById(R.id.name);
        name.setText(publicizeConnection.getExternalDisplayName());

        return rowView;
    }
}
