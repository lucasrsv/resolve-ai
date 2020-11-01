package com.android.resolveai

import java.util.*

data class Post(
    val postId: Int = 0,
    val postUserId: Int = 0,
    val postTitle: String = "",
    val postDescription: String = "",
    val postLocale: String = "",
    val postDate: Date?,
    val postComments: List<Comment>)