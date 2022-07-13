package org.wordpress.android.ui.mysite.jetpackbadge

sealed class JetpackPoweredDialogAction {
    object OpenPlayStore : JetpackPoweredDialogAction()
    object DismissDialog : JetpackPoweredDialogAction()
}
