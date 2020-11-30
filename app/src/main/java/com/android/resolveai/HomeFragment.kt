package com.android.resolveai

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.android.resolveai.databinding.FragmentHomeBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.comment_item.view.*
import kotlinx.android.synthetic.main.report_item.view.*


class HomeFragment : Fragment() {
    private lateinit var recyclerReport: RecyclerView
    private lateinit var recyclerComment: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var options: FirebaseRecyclerOptions<Post>
    private lateinit var commentsOptions: FirebaseRecyclerOptions<Comment>
    private lateinit var firebaseQuery: Query
    private lateinit var reportAdapter: FirebaseRecyclerAdapter<Post, ReportViewHolder>
    private lateinit var commentsAdapter: FirebaseRecyclerAdapter<Comment, CommentsViewHolder>
    private var firstTimeListening = true
    private var onItemClick: (Int) -> Unit = {}
    private lateinit var binding: FragmentHomeBinding

    interface CellClickListener {
        fun onCellClickListener()
    }

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
        if (!firstTimeListening) {
            commentsAdapter.startListening()
        }
        firstTimeListening = false
    }

    override fun onStop() {
        reportAdapter.stopListening()
        //commentsAdapter.stopListening()
        super.onStop()
    }

    private  inner class ReportViewHolder(reportView: View) : RecyclerView.ViewHolder(reportView) {
        var reportTitle: MaterialTextView = reportView.reportTitle
        var reportDescription: MaterialTextView = reportView.reportDescription
        var reportImage: ImageView = reportView.reportImage
        var reportDate: MaterialTextView = reportView.reportDate
        var reportLocal: MaterialTextView = reportView.reportLocal
        var reportComments: RecyclerView = reportView.comments_recycler_view
        var commentButton = reportView.sendCommentButon
        var commentText = reportView.commentInput.text.toString()

    }

    private fun recyclerView() {
        reportAdapter = object : FirebaseRecyclerAdapter<Post, ReportViewHolder>(options) {
            var onItemClick: ((Post) -> Unit)? = null
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
                holder.reportLocal.text = getString(R.string.dateLocal, model.postLocale)
                ImageFromUrl().getImageFromURL(model.postImageUrl, holder.reportImage)

                //Open the report locale in google maps
                holder.reportLocal.setOnClickListener {
                    val latitude = model.postLatitude
                    val longitude = model.postLongitude
                    val mapIntentUri = Uri.parse("geo:$latitude, $longitude")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                }

                holder.commentButton.setOnClickListener {
                    sendCommentToFirebase(holder.itemView.commentInput.text.toString(), model.postId)
                }
                commentsOptions = FirebaseRecyclerOptions.Builder<Comment>()
                        .setQuery(database.child("reports").child(model.postId).child("postComments"), Comment::class.java)
                        .build()
                commentsRecyclerView(model)
                recyclerComment = holder.reportComments.apply {
                    layoutManager = LinearLayoutManager(activity)
                    adapter = commentsAdapter
                }
                commentsAdapter.startListening()
            }
        }
    }


    private inner class CommentsViewHolder(commentsView: View) : RecyclerView.ViewHolder(commentsView) {
        var userName: MaterialTextView = commentsView.userName
        var comment: MaterialTextView = commentsView.comment
    }

    private fun commentsRecyclerView(model: Post) {
        commentsAdapter = object : FirebaseRecyclerAdapter<Comment, CommentsViewHolder>(commentsOptions) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
                val commentView = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)

                return CommentsViewHolder(commentView)

            }

            override fun onBindViewHolder(holder: CommentsViewHolder, position: Int, model: Comment) {
                holder.userName.text = model.commentUserName
                holder.comment.text = model.commentText
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

    private fun sendCommentToFirebase(commentText: String, postId: String) {
        var comment = Comment(
                commentText = commentText,
                commentUserId = auth.currentUser!!.uid
        )
        database.child("users").child(auth.currentUser!!.uid).addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        comment.commentUserName = snapshot.child("userName").value as String
                        val key = database.child("reports").child(postId).child("postComments").push().key.toString()
                        comment.commentId = key
                        database.child("reports").child(postId).child("postComments").child(key).setValue(comment)
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                }
        )
    }
}

