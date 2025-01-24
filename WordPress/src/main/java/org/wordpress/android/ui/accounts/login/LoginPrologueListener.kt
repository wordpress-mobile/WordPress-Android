package org.wordpress.android.ui.accounts.login

import android.content.Context

interface LoginPrologueListener {
    // Login Prologue callbacks
    fun showWPcomLoginScreen(context: Context)
    fun loginViaSiteAddress()
}
