package com.android.resolveai

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.resolveai.databinding.FragmentReportBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity.RESULT_ERROR
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_report.*
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.min

class ReportFragment : Fragment() {
    private lateinit var binding: FragmentReportBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var dateField: TextInputLayout
    private lateinit var placesClient: PlacesClient
    private val AUTOCOMPLETE_REQUEST_CODE = 1

    private val PICK_IMAGE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        //Need to remove the API Key from here
        Places.initialize(requireContext(), "AIzaSyBuYTBbLQaNnh3gEIsB1N2hrMi6f_5MYYQ")
        placesClient = Places.createClient(requireContext())

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportBinding.inflate(inflater, container, false)
        binding.dateInput.let {
            it.inputType = InputType.TYPE_NULL
            it.keyListener = null
        }
        binding.reportLocalInput.let {
            it.inputType = InputType.TYPE_NULL
            it.keyListener = null
        }
        binding.sendReportButton.setOnClickListener {
            if (binding.dateInput.text.toString() == "" ||
                binding.reportTitleInput.text.toString() == "" ||
                binding.reportDescriptionInput.text.toString() == "" ||
                binding.reportLocalInput.text.toString() == "") {
                val toast = Toast.makeText(context, "Preencha todos os campos para enviar a denúncia.", Toast.LENGTH_LONG)
                toast.show()
            } else {
                sendReportToFirebase()
                val toast = Toast.makeText(context, "Denúncia enviada.", Toast.LENGTH_LONG)
                toast.show()
            }
        }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        calendar.clear()
        calendar.timeInMillis = today

        val builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione a data de início do problema.")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(
                        CalendarConstraints.Builder().setEnd(calendar.timeInMillis).build()
                )
        val materialDatePicker = builder.build()

        binding.dateInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                materialDatePicker.show(parentFragmentManager, "DATE_PICKER")
            }
        }

        binding.reportLocalInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(requireActivity().baseContext)
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
            }
        }

        materialDatePicker.addOnPositiveButtonClickListener {
            binding.dateInput.setText(materialDatePicker.headerText, TextView.BufferType.EDITABLE)
            binding.dateInput.clearFocus()
        }
        materialDatePicker.addOnNegativeButtonClickListener { binding.dateInput.clearFocus() }

        binding.openGalleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE)
        }

        binding.root.post {
            val mainButtonHeight = binding.addImageButton.height
            val cameraButtonHeight = binding.openCameraButton.height
            val galleryButtonHeight = binding.openGalleryButton.height
            binding.addImageButton.y += mainButtonHeight/2
            binding.openCameraButton.y += cameraButtonHeight/2
            binding.openGalleryButton.y += galleryButtonHeight/2
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            val imgUri = data?.data
            val imgStream = imgUri?.let { context?.contentResolver?.openInputStream(it) }
            val imgStream2 = imgUri?.let { context?.contentResolver?.openInputStream(it) }
            val targetW = binding.reportImage.width
            val targetH = binding.reportImage.height
            val image = BitmapFactory.decodeStream(imgStream2)
            Log.d("Target W/H ", "$targetW of width, and $targetH of height")
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                val imgW = outWidth //Retornando 0 (?)
                val imgH = outHeight
                val scaleFactor = min(image.width / targetW, image.height / targetH)
                Log.d("Img W/H ", "$imgW of width, and $imgH of height")
                inJustDecodeBounds = false
                //inSampleSize = scaleFactor


                var sampleSize = 1
                if (image.height > targetH || image.width > targetW) {
                    val halfHeight: Int = image.height / 2
                    val halfWidth: Int = image.width / 2
                    // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                    // height and width larger than the requested height and width.
                    while (halfHeight / sampleSize >= targetH && halfWidth / sampleSize >= targetW) {
                        sampleSize *= 2
                    }
                }
                inSampleSize = sampleSize
                Log.d("insmaplesize", sampleSize.toString())
            }
            BitmapFactory.decodeStream(imgStream).also { bitmap ->
                binding.reportImage.setImageBitmap(bitmap)
            }
        } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                    binding.reportLocalInput.setText(place?.name, TextView.BufferType.EDITABLE)
                }
                RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.d("Places_Error: ", status.statusMessage!!)
                    }
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun sendImageToFirebase(key: String) {
        val storageRef = storage.reference
        val databaseRef = database.reference
        val imgBitmap = (reportImage.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imgData = baos.toByteArray()
        storageRef.child(key).child("reportImage.png").putBytes(imgData)
                .addOnSuccessListener {
                    storageRef.child(key).child("reportImage.png").downloadUrl.addOnSuccessListener {
                        databaseRef.child("reports").child(key).child("postImageUrl").setValue(it.toString())
                    }
                }

    }

    private fun sendReportToFirebase() {
        val databaseRef = database.reference
        val post = Post(
                postUserId = auth.currentUser!!.uid,
                postTitle = binding.reportTitleInput.text.toString(),
                postDescription = binding.reportDescriptionInput.text.toString(),
                postLocale = binding.reportLocalInput.text.toString(),
                postProblemDate = binding.dateInput.text.toString(),
                postDate = System.currentTimeMillis(),
        )
        val key = databaseRef.child("reports").push().key.toString()
        databaseRef.child("reports").child(key).setValue(post).addOnSuccessListener {
            sendImageToFirebase(key)
        }
    }



}