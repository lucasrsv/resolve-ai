package com.android.resolveai

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.android.resolveai.databinding.FragmentReportBinding
import com.google.android.gms.maps.model.LatLng
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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_report.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class ReportFragment : Fragment() {
    private lateinit var binding: FragmentReportBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var dateField: TextInputLayout
    private lateinit var placesClient: PlacesClient
    private lateinit var latLng: LatLng
    private lateinit var photoPath: String
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private val REQUEST_IMAGE_CAPTURE = 2

    private val PICK_IMAGE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        Places.initialize(requireContext(), "AIzaSyBuYTBbLQaNnh3gEIsB1N2hrMi6f_5MYYQ")
        placesClient = Places.createClient(requireContext())

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportBinding.inflate(inflater, container, false)

        // This is because we don't want the keyboard to open when the user click on those buttons
        binding.dateInput.let {
            it.inputType = InputType.TYPE_NULL
            it.keyListener = null
        }
        binding.reportLocalInput.let {
            it.inputType = InputType.TYPE_NULL
            it.keyListener = null
        }

        binding.sendReportButton.setOnClickListener {
            // Check if there's any empty input before sending it
            if (binding.dateInput.text.toString() == "" ||
                binding.reportTitleInput.text.toString() == "" ||
                binding.reportDescriptionInput.text.toString() == "" ||
                binding.reportLocalInput.text.toString() == "") {
                val toast = Toast.makeText(context, "Preencha todos os campos para enviar a denúncia.", Toast.LENGTH_LONG)
                toast.show()
            } else {
                sendReportToFirebase()
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

        // We're using setOnFocusChangeListener because there's an issue with the buttons that requires the user to click on it two times
        // So, by using setOnFocusChangeListener, the user won't need to click on it more than once
        binding.dateInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                materialDatePicker.show(parentFragmentManager, "DATE_PICKER")
            }
        }

        binding.reportLocalInput.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // Use Places API to get the report location
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(requireActivity().baseContext)
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
            }
        }

        //If the user chose a date
        materialDatePicker.addOnPositiveButtonClickListener {
            binding.dateInput.setText(materialDatePicker.headerText, TextView.BufferType.EDITABLE)
            binding.dateInput.clearFocus()
        }
        materialDatePicker.addOnNegativeButtonClickListener { binding.dateInput.clearFocus() }

        // Those are just fadeIn and fadeOut animations to show/hide the gallery and camera buttons
        binding.addImageButton.setOnClickListener {
            if (binding.openGalleryButton.visibility == View.INVISIBLE) {
                val fadeInGalleryButton = ObjectAnimator.ofFloat(binding.openGalleryButton, "alpha", 0f, 1f).apply {
                    duration = 250
                    startDelay = 50
                    doOnStart {
                        binding.openGalleryButton.visibility = View.VISIBLE
                        binding.addImageButton.setImageResource(R.drawable.ic_clear)
                    }
                }
                val fadeInCameraButton = ObjectAnimator.ofFloat(binding.openCameraButton, "alpha", 0f, 1f).apply {
                    duration = 250
                    doOnStart { binding.openCameraButton.visibility = View.VISIBLE }
                }
                AnimatorSet().apply {
                    playTogether(fadeInGalleryButton, fadeInCameraButton)
                    start()
                }
            } else {
                val fadeOutGalleryButton = ObjectAnimator.ofFloat(binding.openGalleryButton, "alpha", 1f, 0f).apply {
                    duration = 250
                    doOnStart { binding.addImageButton.setImageResource(R.drawable.ic_attach_file) }
                    doOnEnd { binding.openGalleryButton.visibility = View.INVISIBLE }
                }
                val fadeOutCameraButton = ObjectAnimator.ofFloat(binding.openCameraButton, "alpha", 1f, 0f).apply {
                    duration = 250
                    startDelay = 50
                    doOnEnd { binding.openGalleryButton.visibility = View.INVISIBLE }
                }
                AnimatorSet().apply {
                    playTogether(fadeOutGalleryButton, fadeOutCameraButton)
                    start()
                }
            }
        }

        // Open the camera activity and let the user take a picture
        binding.openCameraButton.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                try {
                    takePictureIntent.also {
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            Toast.makeText(context, "Houve um erro com o arquivo da foto.", Toast.LENGTH_LONG).show()
                            null
                        }
                        photoFile?.also {
                             val photoURI: Uri = FileProvider.getUriForFile(
                                    requireContext(),
                                    "com.resolveai.android.fileprovider",
                                    it
                            )
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                        }
                    }
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Não foi possível abrir a câmera.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Let the user choose a picture from gallery
        binding.openGalleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, PICK_IMAGE)
        }

        // Set new y coordinates to the buttons so they will be one on top of the other
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
        // Set the chosen image (from the gallery) as the report image
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            val imgUri = data?.data
            val imgStream = imgUri?.let { context?.contentResolver?.openInputStream(it) }
            BitmapFactory.decodeStream(imgStream).also { bitmap ->
                binding.reportImage.setImageBitmap(bitmap)
            }
        } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE) { // Get the user location, including latitude and longitude
            when (resultCode) {
                RESULT_OK -> {
                    val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                    binding.reportLocalInput.setText(place?.name, TextView.BufferType.EDITABLE)
                    latLng = place!!.latLng!! //Latitude and longitude
                }
                RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.d("Places_Error: ", status.statusMessage!!)
                        Toast.makeText(activity, "Ocorreu um erro ao salvar a localização.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Set the image the user took from the camera as the report image
            BitmapFactory.decodeFile(photoPath).also { bitmap ->
                binding.reportImage.setImageBitmap(bitmap)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDirectory: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDirectory
        ).apply {
            photoPath = absolutePath
        }
    }

    private fun sendImageToGallery() {
        val file = File(photoPath)
        MediaScannerConnection.scanFile(context, arrayOf(file.toString()), arrayOf(file.name), null)
    }

    private fun sendImageToFirebase(key: String) {
        val storageRef = storage.reference
        val databaseRef = database.reference
        val imgBitmap = (reportImage.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imgData = baos.toByteArray()
        storageRef.child(key).child("reportImage.png").putBytes(imgData)
                .addOnSuccessListener { //Image was succesfully sent to Firestore
                    storageRef.child(key).child("reportImage.png").downloadUrl.addOnSuccessListener {//The image url was succesfully downloaded from Firestore
                        databaseRef.child("reports").child(key).child("postImageUrl").setValue(it.toString()).addOnSuccessListener {//The img url is sent to the Firebase Database
                            //Now, everything is ok: the report data (including image url) was sent to the database
                            //And the image was sent to the storage
                            val toast = Toast.makeText(context, "Denúncia enviada.", Toast.LENGTH_LONG)
                            toast.show()
                        }
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
                postLatitude = latLng.latitude,
                postLongitude = latLng.longitude
        )
        val key = databaseRef.child("reports").push().key.toString()
        post.postId = key
        // Send the post to Firebase Database and Image to Firebase Storage
        databaseRef.child("reports").child(key).setValue(post).addOnSuccessListener {
            sendImageToFirebase(key)
        }
    }



}