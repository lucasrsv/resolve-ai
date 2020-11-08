package com.android.resolveai

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import kotlinx.android.synthetic.main.report_item.view.*

class ReportAdapter(options: FirebaseRecyclerOptions<Post>) :
    FirebaseRecyclerAdapter<Post, ReportAdapter.ReportViewHolder>(options) {

    class ReportViewHolder(reportView: View) : RecyclerView.ViewHolder(reportView) {
        var reportTitle = reportView.reportTitle
        var reportDescription = reportView.reportDescription
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val reportView = LayoutInflater.from(parent.context).inflate(R.layout.report_item, parent, false)
        Log.d("chamou?", "sim")
        return ReportViewHolder(reportView)
    }


    override fun onBindViewHolder(holder: ReportViewHolder, position: Int, report: Post) {
        holder.reportTitle.text = report.postTitle
        holder.reportDescription.text = report.postDescription
        Log.d("holder", holder.reportTitle.text.toString())
    }
}