package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.List;

/**
 * Displays list of {@link String}'s each with a {@link WPRadioButton}.
 */
public class RadioButtonListView extends ListView {
    public RadioButtonListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChoiceMode(CHOICE_MODE_SINGLE);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        setItemChecked(position, true);
    }

    public static class RadioButtonListAdapter extends ArrayAdapter<String> {
        public RadioButtonListAdapter(Context context, List<String> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.radio_button_list_item, null);
            }

            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            if (titleView != null) {
                titleView.setText(getItem(position));
            }

            return convertView;
        }
    }
}
