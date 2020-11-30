package com.android.resolveai

data class User(
    val userId: String? = "",
    val userName: String? = "",
    val userEmail: String? = "",
    val userAddress: String? = "",
    val userPhone: String? ="",
    val userNotifications: List<Post>,
    val userPosts: List<Post>,
    val userComments: List<Comment>
)