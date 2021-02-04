package org.wordpress.android.ui.people;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.RoleUtils;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.people.WPEditTextWithChipsOutlined.ItemValidationState;
import org.wordpress.android.ui.people.WPEditTextWithChipsOutlined.ItemsManagerInterface;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils.ValidateUsernameCallback.ValidationResult;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static com.google.android.material.textfield.TextInputLayout.END_ICON_DROPDOWN_MENU;
import static com.google.android.material.textfield.TextInputLayout.END_ICON_NONE;

public class PeopleInviteFragment extends Fragment implements RoleSelectDialogFragment.OnRoleSelectListener,
        PeopleManagementActivity.InvitationSender {
    private static final String URL_USER_ROLES_DOCUMENTATION = "https://en.support.wordpress.com/user-roles/";
    private static final String FLAG_SUCCESS = "SUCCESS";
    private static final String KEY_USERNAMES = "usernames";
    private static final String KEY_SELECTED_ROLE = "selected-role";

    private ArrayList<String> mUsernames = new ArrayList<>();
    private final HashMap<String, String> mUsernameResults = new HashMap<>();
    private final Map<String, View> mUsernameErrorViews = new Hashtable<>();

    private WPEditTextWithChipsOutlined mUsernamesEmails;
    private AutoCompleteTextView mRoleTextView;
    private TextInputLayout mRoleContainer;
    private EditText mCustomMessageEditText;

    private List<RoleModel> mInviteRoles;
    private String mCurrentRole;
    private String mCustomMessage = "";
    private boolean mInviteOperationInProgress = false;
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;

    public static PeopleInviteFragment newInstance(SiteModel site) {
        PeopleInviteFragment peopleInviteFragment = new PeopleInviteFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        peopleInviteFragment.setArguments(bundle);
        return peopleInviteFragment;
    }

    private void updateSiteOrFinishActivity() {
        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        } else {
            mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.people_invite, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setEnabled(!mInviteOperationInProgress); // here pass the index of send menu item
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentRole != null) {
            outState.putString(KEY_SELECTED_ROLE, mCurrentRole);
        }
        outState.putStringArrayList(KEY_USERNAMES, new ArrayList<>(mUsernamesEmails.getChipsStrings()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        updateSiteOrFinishActivity();
        mInviteRoles = RoleUtils.getInviteRoles(mSiteStore, mSite, this);

        if (savedInstanceState != null) {
            mCurrentRole = savedInstanceState.getString(KEY_SELECTED_ROLE);
            ArrayList<String> retainedUsernames = savedInstanceState.getStringArrayList(KEY_USERNAMES);
            if (retainedUsernames != null) {
                mUsernames.clear();
                mUsernames.addAll(retainedUsernames);
            }
        }

        // retain this fragment across configuration changes
        // WARNING: use setRetainInstance wisely. In this case we need this to be able to get the
        // results of network connections in the same fragment if going through a configuration change
        // (for example, device rotation occurs). Given the simplicity of this particular use case
        // (the fragment state keeps only a couple of EditText components and the SAVE button, it is
        // OK to use it here.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.people_invite_fragment, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar_main);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.invite_people);
        }

        return rootView;
    }

    @Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        mUsernamesEmails = getView().findViewById(R.id.user_names_emails);
        mRoleContainer = getView().findViewById(R.id.role_container);
        mRoleTextView = getView().findViewById(R.id.role);
        mCustomMessageEditText = (EditText) getView().findViewById(R.id.message);

        if (TextUtils.isEmpty(mCurrentRole)) {
            mCurrentRole = getDefaultRole();
        }

        mUsernamesEmails.setItemsManager(new ItemsManagerInterface() {
            @Override public void onRemoveItem(@NotNull String item) {
                removeUsername(item);
            }

            @Override public void onAddItem(@NotNull String item) {
                addUsername(item, null);
            }
        });

        // if mUsernamesEmails is not empty, this means fragment retained itself
        // if mUsernamesEmails is empty, but we have manually retained usernames, this means that fragment was destroyed
        // and we need to recreate manually added views and revalidate usernames
        if (mUsernamesEmails.hasChips()) {
            mUsernameErrorViews.clear();
            populateUsernameChips(new ArrayList<>(mUsernamesEmails.getChipsStrings()));
        } else if (!mUsernames.isEmpty()) {
            populateUsernameChips(new ArrayList<>(mUsernames));
        }

        mRoleTextView.setShowSoftInputOnFocus(false);
        mRoleTextView.setInputType(EditorInfo.TYPE_NULL);
        mRoleTextView.setKeyListener(null);
        refreshRoleTextView();

        if (mInviteRoles.size() > 1) {
            mRoleContainer.setEndIconMode(END_ICON_DROPDOWN_MENU);
            mRoleTextView.setOnClickListener(v -> RoleSelectDialogFragment.show(PeopleInviteFragment.this, 0, mSite));
            mRoleTextView.setFocusable(true);
            mRoleTextView.setFocusableInTouchMode(true);
        } else {
            mRoleContainer.setEndIconMode(END_ICON_NONE);
            mRoleTextView.setOnClickListener(null);
            mRoleTextView.setFocusable(false);
            mRoleTextView.setFocusableInTouchMode(false);
        }

        mRoleContainer.setEndIconOnClickListener(null);
        mRoleContainer.setEndIconCheckable(false);

        MaterialTextView moreInfo = (MaterialTextView) getView().findViewById(R.id.learn_more);
        moreInfo.setOnClickListener(
                v -> ActivityLauncher.openUrlExternal(v.getContext(), URL_USER_ROLES_DOCUMENTATION)
        );

        mCustomMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCustomMessage = mCustomMessageEditText.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // important for accessibility - talkback
        getActivity().setTitle(R.string.invite_people);
    }

    private void resetEditTextContent(EditText editText) {
        if (editText != null) {
            editText.setText("");
        }
    }

    private String getDefaultRole() {
        if (mInviteRoles.isEmpty()) {
            return null;
        }
        return mInviteRoles.get(0).getName();
    }

    private void populateUsernameChips(Collection<String> usernames) {
        if (usernames != null && usernames.size() > 0) {
            validateAndStyleUsername(usernames, null);
        }
    }

    private void addUsername(@NotNull String username, ValidationEndListener validationEndListener) {
        if (username.isEmpty() || mUsernamesEmails.containsChip(username)) {
            if (validationEndListener != null) {
                validationEndListener.onValidationEnd();
            }
            return;
        }
        validateAndStyleUsername(Collections.singletonList(username), validationEndListener);
    }

    private void removeUsername(String username) {
        mUsernameResults.remove(username);
        mUsernamesEmails.removeChip(username);
        updateUsernameError(username, null);
    }

    private boolean isUserInInvitees(String username) {
        return mUsernamesEmails.containsChip(username);
    }

    @Override
    public void onRoleSelected(RoleModel newRole) {
        setRole(newRole.getName());

        if (mUsernamesEmails.hasChips()) {
            // clear the username results list and let the 'validate' routine do the updates
            mUsernameResults.clear();

            validateAndStyleUsername(mUsernamesEmails.getChipsStrings(), null);
        }
    }

    private void setRole(String newRole) {
        mCurrentRole = newRole;
        refreshRoleTextView();
    }

    private void refreshRoleTextView() {
        mRoleTextView.setText(RoleUtils.getDisplayName(mCurrentRole, mInviteRoles));
    }

    private void validateAndStyleUsername(Collection<String> usernames,
                                          final ValidationEndListener validationEndListener) {
        List<String> usernamesToCheck = new ArrayList<>();

        for (String username : usernames) {
            if (mUsernameResults.containsKey(username)) {
                String resultMessage = mUsernameResults.get(username);
                styleChip(username, resultMessage);
                updateUsernameError(username, resultMessage);
            } else {
                styleChip(username, null);
                updateUsernameError(username, null);

                usernamesToCheck.add(username);
            }
        }

        if (usernamesToCheck.size() > 0) {
            long wpcomBlogId = mSite.getSiteId();
            PeopleUtils.validateUsernames(usernamesToCheck, mCurrentRole, wpcomBlogId,
                    new PeopleUtils.ValidateUsernameCallback() {
                        @Override
                        public void onUsernameValidation(String username,
                                                         ValidationResult validationResult) {
                            if (!isAdded()) {
                                return;
                            }

                            if (!isUserInInvitees(username)) {
                                // user is removed from invitees before validation
                                return;
                            }

                            final String usernameResultString =
                                    getValidationErrorString(username, validationResult);
                            mUsernameResults.put(username, usernameResultString);

                            styleChip(username, usernameResultString);
                            updateUsernameError(username, usernameResultString);
                        }

                        @Override
                        public void onValidationFinished() {
                            if (validationEndListener != null) {
                                validationEndListener.onValidationEnd();
                            }
                        }

                        @Override
                        public void onError() {
                            // properly style the button
                        }
                    });
        } else {
            if (validationEndListener != null) {
                validationEndListener.onValidationEnd();
            }
        }
    }

    private void styleChip(String username, @Nullable String validationResultMessage) {
        if (!isAdded()) {
            return;
        }

        ItemValidationState resultState = validationResultMessage == null ? ItemValidationState.NEUTRAL
                : (validationResultMessage.equals(FLAG_SUCCESS)
                        ? ItemValidationState.VALIDATED
                        : ItemValidationState.VALIDATED_WITH_ERRORS);

        mUsernamesEmails.addOrUpdateChip(username, resultState);
    }

    private
    @Nullable
    String getValidationErrorString(String username, ValidationResult validationResult) {
        switch (validationResult) {
            case USER_NOT_FOUND:
                return getString(R.string.invite_username_not_found, username);
            case ALREADY_MEMBER:
                return getString(R.string.invite_already_a_member, username);
            case ALREADY_FOLLOWING:
                return getString(R.string.invite_already_following, username);
            case BLOCKED_INVITES:
                return getString(R.string.invite_user_blocked_invites, username);
            case INVALID_EMAIL:
                return getString(R.string.invite_invalid_email, username);
            case USER_FOUND:
                return FLAG_SUCCESS;
        }

        return null;
    }

    private void updateUsernameError(String username, @Nullable String usernameResult) {
        if (!isAdded()) {
            return;
        }

        TextView usernameErrorTextView;
        if (mUsernameErrorViews.containsKey(username)) {
            usernameErrorTextView = (TextView) mUsernameErrorViews.get(username);

            if (usernameResult == null || usernameResult.equals(FLAG_SUCCESS)) {
                // no error so we need to remove the existing error view
                ((ViewGroup) usernameErrorTextView.getParent()).removeView(usernameErrorTextView);
                mUsernameErrorViews.remove(username);
                return;
            }
        } else {
            if (usernameResult == null || usernameResult.equals(FLAG_SUCCESS)) {
                // no error so no need to create a new error view
                return;
            }

            final ViewGroup usernameErrorsContainer = (ViewGroup) getView()
                    .findViewById(R.id.username_errors_container);

            usernameErrorTextView = (TextView) LayoutInflater.from(getActivity())
                                                             .inflate(R.layout.people_invite_error_view,
                                                                     usernameErrorsContainer, false);

            usernameErrorsContainer.addView(usernameErrorTextView);

            mUsernameErrorViews.put(username, usernameErrorTextView);
        }
        usernameErrorTextView.setText(usernameResult);
    }

    private void clearUsernames(Collection<String> usernames) {
        for (String username : usernames) {
            removeUsername(username);
        }

        if (!mUsernamesEmails.hasChips()) {
            setRole(getDefaultRole());
            resetEditTextContent(mCustomMessageEditText);
        }
    }

    @Override
    public void send() {
        if (!isAdded()) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            enableSendButton(true);
            return;
        }

        enableSendButton(false);

        String lastMinuteUser = mUsernamesEmails.getTextIfAvaliableOrNull();

        if (lastMinuteUser != null) {
            addUsername(lastMinuteUser, new ValidationEndListener() {
                @Override
                public void onValidationEnd() {
                    if (!checkAndSend()) {
                        // re-enable SEND button if validation failed
                        enableSendButton(true);
                    }
                }
            });
        } else {
            if (!checkAndSend()) {
                // re-enable SEND button if validation failed
                enableSendButton(true);
            }
        }
    }

    /*
     * returns true if send is attempted, false if validation failed
     * */
    private boolean checkAndSend() {
        if (!isAdded()) {
            return false;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            return false;
        }

        if (!mUsernamesEmails.hasChips()) {
            ToastUtils.showToast(getActivity(), R.string.invite_error_no_usernames);
            return false;
        }

        int invalidCount = 0;
        for (String usernameResultString : mUsernameResults.values()) {
            if (!usernameResultString.equals(FLAG_SUCCESS)) {
                invalidCount++;
            }
        }

        if (invalidCount > 0) {
            ToastUtils.showToast(getActivity(),
                    StringUtils.getQuantityString(getActivity(), 0,
                            R.string.invite_error_invalid_usernames_one,
                            R.string.invite_error_invalid_usernames_multiple,
                            invalidCount));
            return false;
        }

        // set the "SEND" option disabled
        enableSendButton(false);

        long wpcomBlogId = mSite.getSiteId();
        PeopleUtils.sendInvitations(
                new ArrayList<>(mUsernamesEmails.getChipsStrings()), mCurrentRole, mCustomMessage, wpcomBlogId,
                new PeopleUtils.InvitationsSendCallback() {
                    @Override
                    public void onSent(List<String> succeededUsernames, Map<String, String> failedUsernameErrors) {
                        if (!isAdded()) {
                            return;
                        }

                        clearUsernames(succeededUsernames);

                        if (failedUsernameErrors.size() != 0) {
                            clearUsernames(failedUsernameErrors.keySet());

                            for (Map.Entry<String, String> error : failedUsernameErrors.entrySet()) {
                                final String username = error.getKey();
                                final String errorMessage = error.getValue();
                                mUsernameResults.put(username, getString(R.string.invite_error_for_username,
                                        username, errorMessage));
                            }

                            populateUsernameChips(failedUsernameErrors.keySet());

                            ToastUtils.showToast(getActivity(), succeededUsernames.isEmpty()
                                    ? R.string.invite_error_sending
                                    : R.string.invite_error_some_failed);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.invite_sent, ToastUtils.Duration.LONG);
                        }

                        // set the "SEND" option enabled again
                        enableSendButton(true);
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) {
                            return;
                        }
                        ToastUtils.showToast(getActivity(), R.string.invite_error_sending);
                        // set the "SEND" option enabled again
                        enableSendButton(true);
                    }
                });

        return true;
    }

    private void enableSendButton(boolean enable) {
        mInviteOperationInProgress = !enable;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public interface ValidationEndListener {
        void onValidationEnd();
    }
}
