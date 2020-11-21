package com.android.resolveai

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.resolveai.databinding.FragmentHomeBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.android.synthetic.main.report_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class HomeFragment : Fragment() {
    private lateinit var recyclerReport: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var options: FirebaseRecyclerOptions<Post>
    private lateinit var firebaseQuery: Query
    private lateinit var reportAdapter: FirebaseRecyclerAdapter<Post, ReportViewHolder>
    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
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
        var reportDate: MaterialTextView = reportView.reportDate
    }

    private fun recyclerView() {
        reportAdapter = object : FirebaseRecyclerAdapter<Post, ReportViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
                val reportView = LayoutInflater.from(parent.context).inflate(R.layout.report_item, parent, false)
                val dimensions = context!!.resources!!.displayMetrics
                val height = dimensions.heightPixels

                //Get the minimum height of the cardView and sets a clickListener to expand/collapse the cardView
                reportView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        reportView.viewTreeObserver.removeOnPreDrawListener(this)
                        val layoutParams = reportView.layoutParams
                        val minHeight = reportView.height
                        layoutParams.height = minHeight
                        reportView.layoutParams = layoutParams

                        reportView.setOnClickListener {
                            changeCardHeight(reportView, height, minHeight)
                        }
                        return true
                    }
                })
                return ReportViewHolder(reportView)
            }

            override fun onBindViewHolder(holder: ReportViewHolder, position: Int, model: Post) {
                holder.reportTitle.text = model.postTitle
                holder.reportDescription.text = model.postDescription
                holder.reportDate.text = getString(R.string.dateText, model.postProblemDate)
                getImageFromURL(model.postImageUrl, holder.reportImage)
            }
        }
    }

    //Create a connection to get the Image from the URL stored in Firebase Database, through Coroutines
    private fun getImageFromURL(src: String?, reportImage: ImageView) {
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

    //Check if cardView is already expanded or not
    private fun changeCardHeight(reportView: View, height: Int, minHeight: Int) {
        if (reportView.height == minHeight) {
            expandCardView(height, reportView)
        } else {
            collapseCardView(minHeight, reportView)
        }
    }

    //Animate the height of the cardView
    private fun expandCardView(height: Int, reportView: View) {
        val animation = ValueAnimator.ofInt(reportView.measuredHeightAndState, height).apply {
            addUpdateListener {
                val actualHeight = animatedValue
                val layoutParams = reportView.layoutParams
                layoutParams.height = actualHeight as Int
                reportView.layoutParams = layoutParams
            }
        }
        animation.start()
    }

    //Animate the height of the cardView
    private fun collapseCardView(minHeight: Int, reportView: View) {
        val animation = ValueAnimator.ofInt(reportView.measuredHeightAndState, minHeight).apply {
            addUpdateListener {
                val actualHeight = animatedValue
                val layoutParams = reportView.layoutParams
                layoutParams.height = actualHeight as Int
                reportView.layoutParams = layoutParams
            }
        }
        animation.start()
    }
}