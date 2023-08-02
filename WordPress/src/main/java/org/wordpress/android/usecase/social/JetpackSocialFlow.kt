package org.wordpress.android.usecase.social

sealed class JetpackSocialFlow(val name: String) {
    object PostSettings : JetpackSocialFlow("post_settings")

    object PrePublishing : JetpackSocialFlow("pre_publishing")

    object DashboardCard : JetpackSocialFlow("dashboard_card")
}
