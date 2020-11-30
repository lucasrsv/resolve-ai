package com.android.resolveai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ImageFromUrl {
    //Create a connection to get the Image from the URL stored in Firebase Database, through Coroutines
     fun getImageFromURL(src: String?, reportImage: ImageView) {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                val bitmap: Bitmap = BitmapFactory.decodeStream(input)
                withContext(Dispatchers.Main) {
                    reportImage.setImageBitmap(bitmap)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                e.message?.let { Log.e("Exception ", it) }
            } finally {
                connection.disconnect()
            }
        }
    }
}