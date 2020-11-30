package com.android.resolveai

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.database.ServerValue
import java.util.HashMap

data class      Post(
        var postId: String = "",
        val postUserId: String = "",
        val postTitle: String = "",
        val postDescription: String = "",
        val postDate: Long = 0,
        val postProblemDate: String = "",
        val postLocale: String = "",
        val postLatitude: Double = 0.0,
        val postLongitude: Double = 0.0,
        val postLikes: Int = 0,
        val postImageUrl: String = "",
        val postComments: HashMap<String, Comment> = HashMap()
) {
        constructor(postUserId: String?) : this()
        constructor(postProblemLatLng: LatLng) : this()
}

