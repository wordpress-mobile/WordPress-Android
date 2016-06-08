package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.List;

/**
 * Displays list of {@link String}'s each with a {@link WPRadioButton}.
 */
public class RadioButtonListView extends ListView {
    private RadioButtonListAdapter mAdapter;

    public RadioButtonListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChoiceMode(CHOICE_MODE_SINGLE);
    }

    public static class RadioButtonListAdapter extends ArrayAdapter
            implements CompoundButton.OnCheckedChangeListener {
        private int mSelectedPosition;

        public RadioButtonListAdapter(Context context, List<String> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.radio_button_list_item, parent);
            }

            CompoundButton checkable = (CompoundButton) convertView.findViewById(R.id.checkable);
            checkable.setTag(String.valueOf(position));
            checkable.setOnCheckedChangeListener(this);

            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            if (titleView != null) {
                titleView.setText(getItem(position).toString());
            }

            return convertView;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int position = Integer.valueOf(buttonView.getTag().toString());
            if (position < getCount()) {
                mSelectedPosition = position;
            }
        }
    }
}
