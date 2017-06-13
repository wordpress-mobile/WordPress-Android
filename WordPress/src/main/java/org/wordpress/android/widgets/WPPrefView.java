package org.wordpress.android.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;

/**
 * Single preference view which enables building preference screens without relying on Android's
 * built-in preference tools - enables us to be in the charge of the UI and customizing it as we
 * wish without fighting PreferenceFragment, etc.
 */

public class WPPrefView extends LinearLayout implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public enum PrefType {
        TEXT,       // text setting
        TOGGLE,     // boolean setting
        CHECKLIST,  // multi-select setting
        RADIOLIST;  // single-select setting

        public static PrefType fromInt(int value) {
            switch (value) {
                case 1:
                    return TOGGLE;
                case 2:
                    return CHECKLIST;
                case 3:
                    return RADIOLIST;
                default:
                    return TEXT;
            }
        }
    }

    /*
     * listener for when the user changes the preference
     *      TEXT        use prefView.getTextEntry() to retrieve the updated setting
     *      TOGGLE      use prefView.isChecked() to retrieve the updated setting
     *      RADIOLIST   use prefView.getSelectedItem() to retrieve the updated setting
     *      CHECKLIST   use prefView.getSelectedItems() to retrieve the updated setting
     */
    public interface OnPrefChangedListener {
        void onPrefChanged(@NonNull WPPrefView prefView);
    }

    private PrefType mPrefType = PrefType.TEXT;
    private final PrefListItems mListItems = new PrefListItems();

    private ViewGroup mContainer;
    private TextView mHeadingTextView;
    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private SwitchCompat mToggleSwitch;

    private String mTextEntry;
    private String mTextDialogSubtitle;
    private OnPrefChangedListener mListener;

    /*
     * single item when this is a list preference
     */
    public static class PrefListItem {
        private final String mItemName;   // name to display for this item
        private final String mItemValue;  // value for this item (can be same as name)
        private boolean mIsChecked;       // whether this item is checked

        public PrefListItem(@NonNull String itemName, @NonNull String itemValue, boolean isChecked) {
            mItemName = itemName;
            mItemValue = itemValue;
            mIsChecked = isChecked;
        }
        @SuppressWarnings("unused")
        public @NonNull String getItemName() {
            return mItemName;
        }
        public @NonNull String getItemValue() {
            return mItemValue;
        }
    }

    /*
     * all items when this is a list preference (both single- and multi-select)
     */
    public static class PrefListItems extends ArrayList<PrefListItem> {
        private void setCheckedItems(@NonNull SparseBooleanArray checkedItems) {
            for (int i = 0; i < this.size(); i++) {
                this.get(i).mIsChecked = checkedItems.get(i);
            }
        }
        // use this for RADIOLIST prefs to get the single-select item
        private PrefListItem getFirstSelectedItem() {
            for (PrefListItem item: this) {
                if (item.mIsChecked) {
                    return item;
                }
            }
            return null;
        }
        // use this for CHECKLIST prefs to get all selected items
        private @NonNull PrefListItems getSelectedItems() {
            PrefListItems selectedItems = new PrefListItems();
            for (PrefListItem item: this) {
                if (item.mIsChecked) {
                    selectedItems.add(item);
                }
            }
            return selectedItems;
        }
        // use this with RADIOLIST prefs to select only the passed name
        public void setSelectedName(@NonNull String selectedName) {
            for (PrefListItem item: this) {
                item.mIsChecked = StringUtils.equals(selectedName, item.mItemName);
            }
        }
        public boolean removeItems(@NonNull PrefListItems items) {
            boolean isChanged = false;
            for (PrefListItem item: items) {
                int i = indexOfValue(item.getItemValue());
                if (i > -1) {
                    this.remove(i);
                    isChanged = true;
                }
            }
            return isChanged;
        }
        private int indexOfValue(@NonNull String value) {
            for (int i = 0; i < this.size(); i++) {
                if (this.get(i).getItemValue().equals(value)) {
                    return i;
                }
            }
            return -1;
        }
        public boolean containsValue(@NonNull String value) {
            return indexOfValue(value) > -1;
        }
    }

    public WPPrefView(Context context) {
        super(context);
        initView(context, null);
    }

    public WPPrefView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        ViewGroup view = (ViewGroup) inflate(context, R.layout.wppref_view, this);

        mContainer = (ViewGroup) view.findViewById(R.id.container);
        mHeadingTextView = (TextView) view.findViewById(R.id.text_heading);
        mTitleTextView = (TextView) view.findViewById(R.id.text_title);
        mSummaryTextView = (TextView) view.findViewById(R.id.text_summary);
        mToggleSwitch = (SwitchCompat) view.findViewById(R.id.switch_view);

        mContainer.setOnClickListener(this);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.wpPrefView,
                    0, 0);
            try {
                int prefTypeInt = a.getInteger(R.styleable.wpPrefView_wpPrefType, 0);
                String heading = a.getString(R.styleable.wpPrefView_wpHeading);
                String title = a.getString(R.styleable.wpPrefView_wpTitle);
                String summary = a.getString(R.styleable.wpPrefView_wpSummary);
                String dialogSubtitle = a.getString(R.styleable.wpPrefView_wpTextDialogSubtitle);
                boolean showDivider = a.getBoolean(R.styleable.wpPrefView_wpShowDivider, true);

                setPrefType(PrefType.fromInt(prefTypeInt));
                setHeading(heading);
                setTitle(title);
                setSummary(summary);
                setTextDialogSubtitle(dialogSubtitle);

                View divider = view.findViewById(R.id.divider);
                divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
            } finally {
                a.recycle();
            }
        }
    }

    public void setOnPrefChangedListener(OnPrefChangedListener listener) {
        mListener = listener;
    }

    private void doPrefChanged() {
        if (mListener != null) {
            mListener.onPrefChanged(this);
        }
    }

    private void setPrefType(@NonNull PrefType prefType) {
        mPrefType = prefType;

        boolean isToggle = mPrefType == PrefType.TOGGLE;
        mToggleSwitch.setVisibility(isToggle ? View.VISIBLE : View.GONE);
        mToggleSwitch.setOnCheckedChangeListener(isToggle ? this : null);
    }

    /*
     * blue heading text that should appear above the preference when it's the first in a group
     */
    private void setHeading(String heading) {
        mHeadingTextView.setText(heading);
        mHeadingTextView.setVisibility(TextUtils.isEmpty(heading) ? GONE : VISIBLE);
    }

    /*
     * title above the preference and below the optional heading
     */
    private void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    /*
     * optional description that appears below the title
     */
    public void setSummary(String summary) {
        mSummaryTextView.setText(summary);
        mSummaryTextView.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
    }

    /*
     * subtitle on the dialog that appears when the PrefType is TEXT
     */
    private void setTextDialogSubtitle(String subtitle) {
        mTextDialogSubtitle = subtitle;
    }

    /*
     * current entry when the PrefType is TEXT
     */
    public String getTextEntry() {
        return mTextEntry;
    }
    public void setTextEntry(String entry) {
        mTextEntry = entry;
        setSummary(entry);
    }

    /*
     * returns whether or not the switch is checked when the PrefType is TOGGLE
     */
    public boolean isChecked() {
        return mPrefType == PrefType.TOGGLE && mToggleSwitch.isChecked();
    }
    public void setChecked(boolean checked) {
        mToggleSwitch.setChecked(checked);
    }

    /*
     * the list of items when the PrefType is CHECKLIST or RADIOLIST
     */
    public @NonNull PrefListItems getListItems() {
        return mListItems;
    }

    public void setListItems(@NonNull PrefListItems items) {
        mListItems.clear();
        mListItems.addAll(items);
    }

    public PrefListItem getSelectedItem() {
        return mListItems.getFirstSelectedItem();
    }

    public PrefListItems getSelectedItems() {
        return mListItems.getSelectedItems();
    }

    @Override
    public void onClick(View v) {
        switch (mPrefType) {
            case CHECKLIST:
                showCheckListDialog();
                break;
            case RADIOLIST:
                showRadioListDialog();
                break;
            case TEXT:
                showTextDialog();
                break;
            case TOGGLE:
                mToggleSwitch.setChecked(!mToggleSwitch.isChecked());
                break;
        }
    }

    /*
     * user clicked the toggle switch
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        doPrefChanged();
    }

    /*
     * user clicked the view when the PrefType is TEXT - shows a dialog enabling the user
     * to edit the entry
     */
    private void showTextDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.wppref_text_dialog, null);
        final EditText editText = (EditText) customView.findViewById(R.id.edit);
        editText.setText(mSummaryTextView.getText());
        TextView txtSubtitle = (TextView) customView.findViewById(R.id.text_subtitle);
        if (!TextUtils.isEmpty(mTextDialogSubtitle)) {
            txtSubtitle.setText(mTextDialogSubtitle);
        } else {
            txtSubtitle.setVisibility(GONE);
        }

        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setView(customView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setTextEntry(editText.getText().toString());
                        doPrefChanged();
                    }
                })
        .show();
    }

    /*
     * user clicked the view when the PrefType is CHECKLIST - shows a multi-select dialog enabling
     * the user to modify the list
     */
    private void showCheckListDialog() {
        CharSequence[] items = new CharSequence[mListItems.size()];
        boolean[] checkedItems = new boolean[mListItems.size()];
        for (int i = 0; i < mListItems.size(); i++) {
            items[i] = mListItems.get(i).mItemName;
            checkedItems[i] = mListItems.get(i).mIsChecked;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SparseBooleanArray checkedItems =
                                ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                        mListItems.setCheckedItems(checkedItems);
                        doPrefChanged();
                    }
                })
                .setMultiChoiceItems(items, checkedItems, null)
                .show();
    }

    /*
     * user clicked the view when the PrefType is RADIOLIST - shows a single-select dialog enabling
     * the user to choose a different item
     */
    private void showRadioListDialog() {
        CharSequence[] items = new CharSequence[mListItems.size()];
        int selectedPos = 0;
        for (int i = 0; i < mListItems.size(); i++) {
            items[i] = mListItems.get(i).mItemName;
            if (mListItems.get(i).mIsChecked) {
                selectedPos = i;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SparseBooleanArray checkedItems =
                                ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                        mListItems.setCheckedItems(checkedItems);
                        PrefListItem item = mListItems.getFirstSelectedItem();
                        setSummary(item != null ? item.mItemName : "");
                        doPrefChanged();
                    }
                })
                .setSingleChoiceItems(items, selectedPos, null)
                .show();
    }
}
