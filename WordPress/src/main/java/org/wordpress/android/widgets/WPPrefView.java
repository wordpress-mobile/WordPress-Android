package org.wordpress.android.widgets;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Single preference view which enables building preference screens without relying on Android's
 * built-in preference tools - enables us to be in the charge of the UI and customizing it as we
 * wish without fighting PreferenceFragment, etc.
 */

public class WPPrefView extends LinearLayout implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public enum PrefType {
        TEXT, // text setting
        TOGGLE, // boolean setting
        CHECKLIST, // multi-select setting
        RADIOLIST; // single-select setting

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
     * TEXT use prefView.getTextEntry() to retrieve the updated setting
     * TOGGLE use prefView.isChecked() to retrieve the updated setting
     * RADIOLIST use prefView.getSelectedItem() to retrieve the updated setting
     * CHECKLIST use prefView.getSelectedItems() to retrieve the updated setting
     */
    public interface OnPrefChangedListener {
        void onPrefChanged(@NonNull WPPrefView prefView);
    }

    private PrefType mPrefType = PrefType.TEXT;
    private final PrefListItems mListItems = new PrefListItems();

    private TextView mHeadingTextView;
    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private SwitchCompat mToggleSwitch;

    private String mTextEntry;
    private String mTextDialogSubtitle;
    private OnPrefChangedListener mListener;

    private static final String KEY_LIST_ITEMS = "prefview_listitems";
    private static final String KEY_SUPER_STATE = "prefview_super_state";

    /*
     * single item when this is a list preference
     */
    public static class PrefListItem implements Serializable {
        private final String mItemName; // name to display for this item
        private final String mItemValue; // value for this item (can be same as name)
        private boolean mIsChecked; // whether this item is checked

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
            for (PrefListItem item : this) {
                if (item.mIsChecked) {
                    return item;
                }
            }
            return null;
        }

        // use this for CHECKLIST prefs to get all selected items
        private @NonNull PrefListItems getSelectedItems() {
            PrefListItems selectedItems = new PrefListItems();
            for (PrefListItem item : this) {
                if (item.mIsChecked) {
                    selectedItems.add(item);
                }
            }
            return selectedItems;
        }

        // use this with RADIOLIST prefs to select only the passed name
        public void setSelectedName(@NonNull String selectedName) {
            for (PrefListItem item : this) {
                item.mIsChecked = StringUtils.equals(selectedName, item.mItemName);
            }
        }

        public boolean removeItems(@NonNull PrefListItems items) {
            boolean isChanged = false;
            for (PrefListItem item : items) {
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

    /*
     * Wrapper that will allow us to preserve type of PrefListItems when serializing it
     */
    public static class PrefListItemsWrapper implements Serializable {
        private PrefListItems mList;

        public PrefListItems getList() {
            return mList;
        }

        PrefListItemsWrapper(PrefListItems mList) {
            this.mList = mList;
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

    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        ViewGroup view = (ViewGroup) inflate(context, R.layout.wppref_view, this);

        ViewGroup container = view.findViewById(R.id.container);
        mHeadingTextView = view.findViewById(R.id.text_heading);
        mTitleTextView = view.findViewById(R.id.text_title);
        mSummaryTextView = view.findViewById(R.id.text_summary);
        mToggleSwitch = view.findViewById(R.id.switch_view);

        container.setOnClickListener(this);

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

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_LIST_ITEMS, new PrefListItemsWrapper(mListItems));
        bundle.putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            PrefListItemsWrapper listWrapper = (PrefListItemsWrapper) bundle.getSerializable(KEY_LIST_ITEMS);
            if (listWrapper != null) {
                PrefListItems items = listWrapper.getList();
                setListItems(items);
            }
            state = bundle.getParcelable(KEY_SUPER_STATE);
        }
        super.onRestoreInstanceState(state);
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
    public void setHeading(String heading) {
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
            case RADIOLIST:
            case TEXT:
                if (getContext() instanceof Activity) {
                    Activity activity = (Activity) getContext();
                    WPPrefDialogFragment fragment = WPPrefDialogFragment.newInstance(this);
                    activity.getFragmentManager().executePendingTransactions();
                    fragment.show(activity.getFragmentManager(), "pref_dialog_tag");
                }
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
    private Dialog getTextDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        //noinspection InflateParams
        ViewGroup customView = (ViewGroup) inflater.inflate(R.layout.wppref_text_dialog, null);
        final EditText editText = customView.findViewById(R.id.edit);
        editText.setText(mSummaryTextView.getText());
        TextView txtSubtitle = customView.findViewById(R.id.text_subtitle);
        if (!TextUtils.isEmpty(mTextDialogSubtitle)) {
            txtSubtitle.setText(mTextDialogSubtitle);
        } else {
            txtSubtitle.setVisibility(GONE);
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(mTitleTextView.getText())
                .setView(customView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    setTextEntry(editText.getText().toString());
                    doPrefChanged();
                });
        return builder.create();
    }

    /*
     * user clicked the view when the PrefType is CHECKLIST - shows a multi-select dialog enabling
     * the user to modify the list
     */
    private Dialog getCheckListDialog() {
        CharSequence[] items = new CharSequence[mListItems.size()];
        boolean[] checkedItems = new boolean[mListItems.size()];
        for (int i = 0; i < mListItems.size(); i++) {
            items[i] = mListItems.get(i).mItemName;
            checkedItems[i] = mListItems.get(i).mIsChecked;
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    SparseBooleanArray userCheckedItems =
                            ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                    mListItems.setCheckedItems(userCheckedItems);
                    doPrefChanged();
                })
                .setMultiChoiceItems(items, checkedItems, null);
        return builder.create();
    }

    /*
     * user clicked the view when the PrefType is RADIOLIST - shows a single-select dialog enabling
     * the user to choose a different item
     */
    private Dialog getRadioListDialog() {
        CharSequence[] items = new CharSequence[mListItems.size()];
        int selectedPos = 0;
        for (int i = 0; i < mListItems.size(); i++) {
            items[i] = mListItems.get(i).mItemName;
            if (mListItems.get(i).mIsChecked) {
                selectedPos = i;
            }
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    SparseBooleanArray checkedItems =
                            ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                    mListItems.setCheckedItems(checkedItems);
                    PrefListItem item = mListItems.getFirstSelectedItem();
                    setSummary(item != null ? item.mItemName : "");
                    doPrefChanged();
                })
                .setSingleChoiceItems(items, selectedPos, null);
        return builder.create();
    }

    public static class WPPrefDialogFragment extends DialogFragment {
        private int mPrefViewId;
        private static final String ARG_PREF_VIEW_ID = "pref_view_ID";

        public WPPrefDialogFragment() {
            // noop
        }

        public static WPPrefDialogFragment newInstance(@NonNull WPPrefView prefView) {
            WPPrefDialogFragment frag = new WPPrefDialogFragment();

            Bundle args = new Bundle();
            args.putInt(ARG_PREF_VIEW_ID, prefView.getId());
            frag.setArguments(args);

            return frag;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mPrefViewId = getArguments().getInt(ARG_PREF_VIEW_ID);
        }

        private
        @Nullable
        WPPrefView getPrefView() {
            if (getActivity() != null) {
                return (WPPrefView) getActivity().findViewById(mPrefViewId);
            }
            return null;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WPPrefView prefView = getPrefView();
            if (prefView != null) {
                switch (prefView.mPrefType) {
                    case TEXT:
                        return prefView.getTextDialog();
                    case RADIOLIST:
                        return prefView.getRadioListDialog();
                    case CHECKLIST:
                        return prefView.getCheckListDialog();
                }
            }
            return super.onCreateDialog(savedInstanceState);
        }
    }
}
