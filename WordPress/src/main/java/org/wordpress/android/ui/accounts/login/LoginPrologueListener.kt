package org.wordpress.android.ui.accounts.login

import java.util.ArrayList

interface LoginPrologueListener {
    // Login Prologue callbacks
    fun showEmailLoginScreen()
    fun doStartSignup()
    fun loginViaSiteAddress()
    fun loggedInViaSignup(oldSitesIds: ArrayList<Int?>?)
    fun newUserCreatedButErrored(email: String?, password: String?)
}
