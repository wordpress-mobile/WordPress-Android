package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.helpers.Debouncer;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SiteCreationDomainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_INPUT = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    private static final int GET_SUGGESTIONS_INTERVAL_MS = 400;

    public interface OnAdapterListener {
        void onKeywordsChange(String keywords);

        void onSelectionChange(String domain);
    }

    private boolean mIsLoading;
    private String mInitialKeywords;
    private List<String> mSuggestions;
    private OnAdapterListener mOnAdapterListener;

    private String mCarryOverDomain;
    private String mSelectedDomain;
    private boolean mNeedExtraLineForSelectedDomain;

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class InputViewHolder extends RecyclerView.ViewHolder {
        private final EditText mInput;
        private final TextWatcher mTextWatcher;
        private final View mProgressBar;

        private boolean mIsDetached;
        private boolean mKeepFocus;

        private InputViewHolder(View itemView) {
            super(itemView);
            this.mInput = itemView.findViewById(R.id.input);
            this.mProgressBar = itemView.findViewById(R.id.progress_bar);

            this.mTextWatcher = new TextWatcher() {
                private Debouncer mDebouncer = new Debouncer();

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(final Editable editable) {
                    final String text = editable.toString();
                    mDebouncer.debounce(Void.class, new Runnable() {
                        @Override
                        public void run() {
                            // post the action on an properly initialised Handler so UI thread operations can succeed
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mOnAdapterListener.onKeywordsChange(text);
                                }
                            });
                        }
                    }, GET_SUGGESTIONS_INTERVAL_MS, TimeUnit.MILLISECONDS);
                }
            };
        }
    }

    private static class DomainViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton mRadioButton;

        private DomainViewHolder(View itemView) {
            super(itemView);
            mRadioButton = itemView.findViewById(R.id.radio_button);
        }
    }

    SiteCreationDomainAdapter(Context context, String initialKeywords, OnAdapterListener onAdapterListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        // Stable IDs so the edittext doesn't lose focus on refresh
        setHasStableIds(true);

        mInitialKeywords = initialKeywords;
        mOnAdapterListener = onAdapterListener;
    }

    void setData(boolean isLoading, String carryOverDomain, String selectedDomain, List<String> suggestions) {
        if (isLoading != mIsLoading) {
            notifyItemChanged(1);
        }

        mIsLoading = isLoading;
        mSuggestions = suggestions;
        mCarryOverDomain = carryOverDomain;
        mSelectedDomain = selectedDomain;

        mOnAdapterListener.onSelectionChange(mSelectedDomain);

        mNeedExtraLineForSelectedDomain = carryOverDomain != null
                                          && (suggestions == null || !suggestions.contains(carryOverDomain));

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return new HeaderViewHolder(LayoutInflater.from(parent.getContext())
                                                          .inflate(R.layout.site_creation_domain_header, parent,
                                                                   false));
            case VIEW_TYPE_INPUT:
                return new InputViewHolder(LayoutInflater.from(parent.getContext())
                                                         .inflate(R.layout.site_creation_domain_input, parent,
                                                                 false));
            case VIEW_TYPE_ITEM:
                return new DomainViewHolder(LayoutInflater.from(parent.getContext())
                                                          .inflate(R.layout.site_creation_domain_item, parent,
                                                                  false));
            default:
                throw new RuntimeException("Unknown view type " + viewType);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if (holder instanceof InputViewHolder) {
            ((InputViewHolder) holder).mIsDetached = true;
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (holder instanceof InputViewHolder) {
            ((InputViewHolder) holder).mIsDetached = false;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        switch (viewType) {
            // case VIEW_TYPE_HEADER:
            // nothing to be bound for VIEW_TYPE_HEADER so, just have this as a comment

            case VIEW_TYPE_INPUT:
                bindInput((InputViewHolder) holder);
                break;
            case VIEW_TYPE_ITEM:
                bindSuggest((DomainViewHolder) holder, position);
                break;
        }
    }

    private void bindInput(final InputViewHolder inputViewHolder) {
        inputViewHolder.mProgressBar.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);

        inputViewHolder.mInput.removeTextChangedListener(inputViewHolder.mTextWatcher);
        if (inputViewHolder.mKeepFocus) {
            inputViewHolder.mInput.requestFocus();
        }
        if (mInitialKeywords != null) {
            inputViewHolder.mInput.setText(mInitialKeywords);
            mInitialKeywords = null;
        }
        inputViewHolder.mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null
                        && event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    mOnAdapterListener.onKeywordsChange(inputViewHolder.mInput.getText().toString());
                }

                // always consume the event so the focus stays in the EditText
                return true;
            }
        });
        inputViewHolder.mInput.addTextChangedListener(inputViewHolder.mTextWatcher);
        inputViewHolder.mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                // when the focus is lost when out-of-view then it means we lost it because of the shadowing.
                // Let's keep a note to restore focus when back-in-view.
                inputViewHolder.mKeepFocus = !focused && inputViewHolder.mIsDetached;
            }
        });
    }

    private void bindSuggest(final DomainViewHolder domainViewHolder, int position) {
        final String suggestion = getItem(position);
        final boolean onSelectedItem = suggestion.equals(mSelectedDomain);
        domainViewHolder.mRadioButton.setChecked(onSelectedItem);
        domainViewHolder.mRadioButton.setText(suggestion);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!onSelectedItem) {
                    mSelectedDomain = suggestion;
                    notifyDataSetChanged();
                    mOnAdapterListener.onSelectionChange(suggestion);
                }
            }
        };

        domainViewHolder.mRadioButton.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        int itemCount = 2; // the header and the input box

        // extra line if the selected domain is not in the list
        itemCount += mNeedExtraLineForSelectedDomain ? 1 : 0;

        itemCount += mSuggestions == null ? 0 : mSuggestions.size();
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (position == 1) {
            return VIEW_TYPE_INPUT;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public long getItemId(int position) {
        return position; // just return the position itself. Items are all unique anyway.
    }

    private String getItem(int position) {
        if (mNeedExtraLineForSelectedDomain) {
            if (position == 2) {
                // return the selected domain if we're on the first item on the listview
                return mCarryOverDomain;
            } else {
                // otherwise return the suggestion from the data
                return mSuggestions.get(position - 3);
            }
        } else {
            return mSuggestions.get(position - 2);
        }
    }
}
