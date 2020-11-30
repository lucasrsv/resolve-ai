package com.android.resolveai

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.resolveai.databinding.FragmentProfileBinding
import com.android.resolveai.databinding.FragmentReportBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        databaseRef = database.reference
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.nameCard.setOnClickListener {
            dialogFragment()
        }
        ImageFromUrl().getImageFromURL(auth.currentUser!!.photoUrl.toString(), binding.userPicture)
        databaseRef.child("users").child(auth.currentUser!!.uid).addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        binding.userName.text = snapshot.child("userName").value as String
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                }
        )

        return binding.root
    }

    private fun dialogFragment() {
        var dialog = AlertDialog.Builder(activity)
        dialog.setTitle("Alerta")
        dialog.setMessage("Oi!")
        dialog.

        dialog.setPositiveButton("Modificar") { _, _ ->
            Toast.makeText(activity, "Oi", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }
}