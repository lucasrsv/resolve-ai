package com.android.resolveai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.resolveai.databinding.FragmentHomeBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.report_item.view.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class HomeFragment : Fragment() {
    private lateinit var recyclerReport: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var options: FirebaseRecyclerOptions<Post>
    private lateinit var firebaseQuery: Query
    private lateinit var reportAdapter: FirebaseRecyclerAdapter<Post, ReportViewHolder>
    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
        firebaseQuery = database.child("reports")
        options = FirebaseRecyclerOptions.Builder<Post>()
                .setQuery(firebaseQuery, Post::class.java)
                .build()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        val storage = Firebase.storage
        val storageRef = storage.reference
        val arianaRef =  storageRef.child("-MLcqzJ0ggIIh7Umqysu/ariana.jpg")
        val ariana = activity?.let { ContextCompat.getDrawable(it, R.drawable.ariana) }
        val bitmap = (ariana as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        var uploadTask = arianaRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Log.d("errado", "eerrado")
        }.addOnSuccessListener {
            Log.d("certo", "certo")
        }

        val url = storageRef.child("-MLcqzJ0ggIIh7Umqysu/ariana.jpg").downloadUrl.addOnSuccessListener {
            Log.d("url", it.toString())
        }
        recyclerView()
        recyclerReport = view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = reportAdapter

        }
        return view
    }

    override fun onStart() {
        super.onStart()
        reportAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        reportAdapter.stopListening()
    }

    private  inner class ReportViewHolder(reportView: View) : RecyclerView.ViewHolder(reportView) {
        var reportTitle: MaterialTextView = reportView.reportTitle
        var reportDescription: MaterialTextView = reportView.reportDescription
        var reportImage: ImageView = reportView.reportImage
    }

    private fun recyclerView() {
        reportAdapter = object : FirebaseRecyclerAdapter<Post, ReportViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
                val reportView = LayoutInflater.from(parent.context).inflate(R.layout.report_item, parent, false)
                return ReportViewHolder(reportView)
            }

            override fun onBindViewHolder(holder: ReportViewHolder, position: Int, model: Post) {
                holder.reportTitle.text = model.postTitle
                holder.reportDescription.text = model.postDescription
                getImageFromURL("https://firebasestorage.googleapis.com/v0/b/resolve-ai-android.appspot.com/o/-MLcqzJ0ggIIh7Umqysu%2Fariana.jpg?alt=media&token=fa06ac08-9c35-48ea-b21e-410e9761ed86", holder.reportImage)
            }
        }
    }


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