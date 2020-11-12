package com.android.resolveai

import java.sql.Timestamp
import java.util.*

data class Post(
        val postId: Int = 0,
        val postUserId: Int = 0,
        val postTitle: String = "",
        val postDescription: String = "",
        val postLocale: String = "",
        val postLikes: Int = 0,
        val postImageUrl: String = "",
        val postComments: List<Comment> = listOf(Comment(
                0,
                0,
                0,
                "a"
        ))
)

