package com.android.resolveai

import com.google.firebase.database.ServerValue

data class Post(
        val postId: String = "",
        val postUserId: String = "",
        val postTitle: String = "",
        val postDescription: String = "",
        val postDate: Long = 0,
        val postProblemDate: String = "",
        val postLocale: String = "",
        val postLikes: Int = 0,
        val postImageUrl: String = "",
        val postComments: List<Comment> = listOf(Comment(
                0,
                0,
                0,
                "a"
        ))
) {
        constructor(postUserId: String?) : this()
}

