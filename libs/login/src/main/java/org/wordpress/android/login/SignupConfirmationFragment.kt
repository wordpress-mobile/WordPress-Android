package org.wordpress.android.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.login_include_email_header.*
import kotlinx.android.synthetic.main.signup_confirmation_screen.*
import kotlinx.android.synthetic.main.toolbar_login.*
import org.wordpress.android.login.util.AvatarHelper.AvatarRequestListener
import org.wordpress.android.login.util.AvatarHelper.loadAvatarFromEmail

class SignupConfirmationFragment : Fragment() {
    private var mLoginListener: LoginListener? = null

    private var mEmail: String? = null
    private var mIsSocialSignup: Boolean = false

    companion object {
        const val TAG = "signup_confirmation_fragment_tag"

        private const val ARG_EMAIL = "ARG_EMAIL"
        private const val ARG_IS_SOCIAL_SIGNUP = "ARG_IS_SOCIAL_SIGNUP"

        @JvmStatic fun newInstance(email: String?): SignupConfirmationFragment {
            return SignupConfirmationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                    putBoolean(ARG_IS_SOCIAL_SIGNUP, false)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        if (context !is LoginListener) {
            throw RuntimeException("$context must implement LoginListener")
        }
        mLoginListener = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mEmail = it.getString(ARG_EMAIL)
            mIsSocialSignup = it.getBoolean(ARG_IS_SOCIAL_SIGNUP)
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.signup_confirmation_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity?)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setTitle(R.string.sign_up_label)
                setDisplayHomeAsUpEnabled(true)
            }
        }

        email.text = mEmail

        val avatarRequestListener = object : AvatarRequestListener {
            override fun onRequestFinished() {
                avatar_progress.visibility = View.GONE
            }
        }

        if (mIsSocialSignup) {
            // TODO Implement social signup confirmation
        } else {
            loadAvatarFromEmail(this, mEmail, gravatar, avatarRequestListener)
            label.setText(R.string.signup_confirmation_magic_link_message)
            signup_confirmation_button.setText(R.string.send_link_by_email)
            signup_confirmation_button.setOnClickListener {
                mLoginListener?.showSignupMagicLink(mEmail)
            }
        }

        if (savedInstanceState == null) {
            // TODO Track screen
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // important for accessibility - talkback
        activity?.setTitle(R.string.signup_confirmation_title)
    }

    override fun onDetach() {
        super.onDetach()
        mLoginListener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            if (mLoginListener != null) {
                // TODO Show help
                // mLoginListener?.helpSignupConfirmationScreen(mEmail)
            }
            return true
        }
        return false
    }
}
