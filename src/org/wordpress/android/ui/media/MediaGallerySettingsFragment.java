package org.wordpress.android.ui.media;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;

public class MediaGallerySettingsFragment extends SherlockFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.media_gallery_settings_fragment, container, false);
        
        LinearLayout colLayout = (LinearLayout) view.findViewById(R.id.media_gallery_num_columns_view);
        
        for (int i = 1; i <= 9; i++) {
            Button button = new Button(getActivity());
            button.setText(i + "");
            colLayout.addView(button);
        }
        
        return view;
    }

}
