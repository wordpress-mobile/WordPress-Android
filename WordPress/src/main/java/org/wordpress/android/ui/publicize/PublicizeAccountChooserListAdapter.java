package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by Will on 6/24/16.
 */
public class PublicizeAccountChooserListAdapter extends ArrayAdapter {
    private boolean mAreAccountsConnected;

    public PublicizeAccountChooserListAdapter(Context context, int resource, PublicizeConnection[] objects, boolean isConnected) {
        super(context, resource, objects);
        mAreAccountsConnected = isConnected;
    }

    public PublicizeAccountChooserListAdapter(Context context, int resource, PublicizeConnection[] objects) {
        this(context, resource, objects, true);
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

        if (mAreAccountsConnected) {
            RadioButton radioButton = (RadioButton) rowView.findViewById(R.id.radio_button);
            radioButton.setVisibility(View.INVISIBLE);
        }

        return rowView;
    }
}
