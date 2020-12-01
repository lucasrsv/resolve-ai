package com.android.resolveai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.android.resolveai.databinding.FragmentProfileBinding
import com.android.resolveai.databinding.FragmentReportBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentProfileBinding
    private lateinit var placesClient: PlacesClient
    private var AUTOCOMPLETE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        databaseRef = database.reference
        auth = FirebaseAuth.getInstance()
        Places.initialize(requireContext(), "AIzaSyBuYTBbLQaNnh3gEIsB1N2hrMi6f_5MYYQ")
        placesClient = Places.createClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Call a dialog with a TextInput
        binding.nameCard.setOnClickListener {
            dialogFragment("name")
        }

        // Call directly the activity provided by Places API. There's no need to open a dialog
        binding.locationCard.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(requireActivity().baseContext)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        // Get the user profile picture
        ImageFromUrl().getImageFromURL(auth.currentUser!!.photoUrl.toString(), binding.userPicture)
        databaseRef.child("users").child(auth.currentUser!!.uid).addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        binding.userName.text = snapshot.child("userName").value as String
                        binding.name.text = snapshot.child("userName").value as String
                        binding.userAddress.text = snapshot.child("userAddress").value as String
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
        )

        return binding.root
    }

    private fun dialogFragment(data: String) {
        var dialog = Dialog(data)
        dialog.show(requireFragmentManager(), "dialog")
    }

    // Handle the result from the activity provided by the Places API
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                    var databaseRef = database.reference
                    // Update the user address
                    databaseRef.child("users").child(auth.uid.toString()).child("userAddress").setValue(place!!.name)
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.d("Places_Error: ", status.statusMessage!!)
                        Toast.makeText(activity, "Ocorreu um erro ao salvar a localização.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}