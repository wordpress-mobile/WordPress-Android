package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
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
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.util.helpers.Debouncer;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SiteCreationDomainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_INPUT = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    public interface OnAdapterListener {
        void onKeywordsChange(String keywords);
        void onSelectionChange(int index, String domain);
    }

    private boolean mIsLoading;
    private String mKeywords = "";
    private List<DomainSuggestionResponse> mSuggestions;
    private OnAdapterListener mOnAdapterListener;

    private int mSelectedDomainSuggestionIndex;

    private Debouncer mDebouncer = new Debouncer();

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class InputViewHolder extends RecyclerView.ViewHolder {
        private final EditText input;
        private final TextWatcher textWatcher;
        private final View progressBar;

        private boolean isDetached;
        private boolean keepFocus;

        private InputViewHolder(View itemView, TextWatcher textWatcher) {
            super(itemView);
            this.input = itemView.findViewById(R.id.input);
            this.textWatcher = textWatcher;
            this.progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }

    private static class DomainViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton radioButton;
        private final TextView textView;

        private DomainViewHolder(View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radio_button);
            textView = itemView.findViewById(R.id.text);
        }
    }

    SiteCreationDomainAdapter(Context context, OnAdapterListener onAdapterListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        // Stable IDs so the edittext doesn't lose focus on refresh
        setHasStableIds(true);

        mOnAdapterListener = onAdapterListener;
    }

    void setData(boolean isLoading, String keywords, int selectedDomainSuggestionIndex,
            List<DomainSuggestionResponse> suggestions) {
        if (isLoading != mIsLoading) {
            notifyItemChanged(1);
        }

        mIsLoading = isLoading;
        mKeywords = keywords;
        mSuggestions = suggestions;
        mSelectedDomainSuggestionIndex = selectedDomainSuggestionIndex;

        mOnAdapterListener.onSelectionChange(mSelectedDomainSuggestionIndex,
                mSuggestions != null ? mSuggestions.get(mSelectedDomainSuggestionIndex).domain_name : null);

        notifyDataSetChanged();
    }

    private void keywordsChanged(String text) {
        mOnAdapterListener.onKeywordsChange(text);
        mOnAdapterListener.onSelectionChange(-1, null);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_header,
                    parent, false);
            return new HeaderViewHolder(itemView);
        } else if (viewType == VIEW_TYPE_INPUT) {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_input,
                    parent, false);
            TextWatcher textWatcher = new TextWatcher() {
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
                            keywordsChanged(text);
                        }
                    }, 400, TimeUnit.MILLISECONDS);
                }
            };
            return new InputViewHolder(itemView, textWatcher);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_item, parent,
                    false);
            return new DomainViewHolder(itemView);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if (holder instanceof InputViewHolder) {
            ((InputViewHolder) holder).isDetached = true;
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (holder instanceof InputViewHolder) {
            ((InputViewHolder) holder).isDetached = false;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        switch (viewType) {
            // case VIEW_TYPE_HEADER:
            //  nothing to be bound for VIEW_TYPE_HEADER so, just have this as a comment

            case VIEW_TYPE_INPUT:
                bindInput((InputViewHolder) holder);
                break;
            case VIEW_TYPE_ITEM:
                bindSuggest((DomainViewHolder) holder, position);
                break;
        }
    }

    private void bindInput(final InputViewHolder inputViewHolder) {
        inputViewHolder.progressBar.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);

        inputViewHolder.input.removeTextChangedListener(inputViewHolder.textWatcher);
        if (inputViewHolder.keepFocus) {
            inputViewHolder.input.requestFocus();
        }
        if (!inputViewHolder.input.getText().toString().equals(mKeywords)) {
            inputViewHolder.input.setText(mKeywords);
        }
        inputViewHolder.input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null
                        && event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    keywordsChanged(inputViewHolder.input.getText().toString());
                }

                // always consume the event so the focus stays in the EditText
                return true;
            }
        });
        inputViewHolder.input.addTextChangedListener(inputViewHolder.textWatcher);
        inputViewHolder.input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                // when the focus is lost when out-of-view then it means we lost it because of the shadowing.
                //  Let's keep a note to restore focus when back-in-view.
                inputViewHolder.keepFocus = !focused && inputViewHolder.isDetached;
            }
        });
    }

    private void bindSuggest(final DomainViewHolder domainViewHolder, int position) {
        final boolean onSelectedItem = position - 2 == mSelectedDomainSuggestionIndex;
        final DomainSuggestionResponse suggestion = getItem(position);
        domainViewHolder.radioButton.setChecked(onSelectedItem);
        domainViewHolder.textView.setText(suggestion.domain_name);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!onSelectedItem) {
                    mSelectedDomainSuggestionIndex = domainViewHolder.getAdapterPosition() - 2;
                    notifyDataSetChanged();
                    mOnAdapterListener.onSelectionChange(mSelectedDomainSuggestionIndex,
                            mSuggestions.get(mSelectedDomainSuggestionIndex).domain_name);
                }
            }
        };

        domainViewHolder.itemView.setOnClickListener(clickListener);
        domainViewHolder.radioButton.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return 2 + (mSuggestions == null ? 0 : mSuggestions.size());
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

    private DomainSuggestionResponse getItem(int position) {
        return mSuggestions.get(position - 2);
    }
}
