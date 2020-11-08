package com.android.resolveai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.android.synthetic.main.report_item.view.*
import java.util.*


class HomeFragment : Fragment() {
    private lateinit var recyclerReport: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var options: FirebaseRecyclerOptions<Post>
    private lateinit var firebaseQuery: Query

    private lateinit var reportAdapter: FirebaseRecyclerAdapter<Post, ReportAdapter.ReportViewHolder>

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

        val view = inflater.inflate(R.layout.fragment_home, container, false)
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

    fun recyclerView() {
        reportAdapter = object : FirebaseRecyclerAdapter<Post, ReportAdapter.ReportViewHolder>(options) {
            inner class ReportViewHolder(reportView: View) : RecyclerView.ViewHolder(reportView) {
                var reportTitle = reportView.reportTitle
                var reportDescription = reportView.reportDescription
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportAdapter.ReportViewHolder {
                val reportView = LayoutInflater.from(parent.context).inflate(com.android.resolveai.R.layout.report_item, parent, false)
                return ReportAdapter.ReportViewHolder(reportView)
            }

            override fun onBindViewHolder(holder: ReportAdapter.ReportViewHolder, position: Int, model: Post) {
                holder.reportTitle.text = model.postTitle
                holder.reportDescription.text = model.postDescription
            }

        }
    }
}