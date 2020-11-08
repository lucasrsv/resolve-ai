package com.android.resolveai

import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

data class Comment(
        val commentId: Int = 0,
        val commentUserId: Int = 0,
        val commentUserName: Int = 0,
        val commentText: String = ""
)