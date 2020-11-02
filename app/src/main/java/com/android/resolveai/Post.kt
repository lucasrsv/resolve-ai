package com.android.resolveai

import java.util.*

data class Post(
    val postId: Int = 0,
    val postUserId: Int = 0,
    val postTitle: String = "",
    val postDescription: String = "",
    val postLocale: String = "",
    val postDate: Date?,
    val postLikes: Int = 0,
    val postComments: List<Comment>)