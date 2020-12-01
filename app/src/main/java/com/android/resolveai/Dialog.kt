package com.android.resolveai

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.dialog.view.*

class Dialog(private var data: String) : DialogFragment() {
    private lateinit var editText: TextInputEditText
    private lateinit var button: MaterialButton
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var view = requireActivity().layoutInflater.inflate(R.layout.dialog, null)
        editText = view.findViewById(R.id.textInput)
        button = view.findViewById(R.id.changeInfoButton)
        button.setOnClickListener {
            var userNewData = editText.text.toString()
            var databaseRef = database.reference
            if (userNewData != "") {
                databaseRef.child("users").child(auth.uid.toString()).child("userName").setValue(userNewData).addOnSuccessListener {
                    dismiss()
                }
            } 
        }

        var dialog = AlertDialog.Builder(activity)
        dialog.setView(view)

        return dialog.create()
    }
}