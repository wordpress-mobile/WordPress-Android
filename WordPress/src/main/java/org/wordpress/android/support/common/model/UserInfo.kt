package org.wordpress.android.support.common.model

data class UserInfo(
    val accessToken: String,
    val userName: String,
    val userEmail: String,
    val avatarUrl: String? = null
)
