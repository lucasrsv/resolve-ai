package com.android.resolveai

import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

data class Comment(
        var commentId: String = "",
        val commentUserId: String = "",
        var commentUserName: String = "",
        val commentText: String = ""
)