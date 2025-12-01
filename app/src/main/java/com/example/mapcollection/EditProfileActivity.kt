package com.example.mapcollection

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mapcollection.databinding.ActivityEditprofileBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditprofileBinding
    private var imageUri: Uri? = null
    private var currentEmail: String? = null

    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                binding.imgUserPhoto.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentEmail = getSharedPreferences("Account", MODE_PRIVATE)
            .getString("LOGGED_IN_EMAIL", null)

        // Prefill with current values passed from MainActivity
        binding.edUserName.setText(intent.getStringExtra("currentUserName").orEmpty())
        binding.edUserLabel.setText(intent.getStringExtra("currentUserLabel").orEmpty())
        binding.edIntroduction.setText(intent.getStringExtra("currentIntroduction").orEmpty())

        binding.imgUserPhoto.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnSave.setOnClickListener {
            val name = binding.edUserName.text.toString().trim()
            val label = binding.edUserLabel.text.toString().trim()
            val intro = binding.edIntroduction.text.toString().trim()
            val email = currentEmail

            if (name.isEmpty() || email.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.error_missing_name_or_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveProfile(email, name, label, intro)
        }
    }

    private fun saveProfile(email: String, name: String, label: String, intro: String) {
        val userData = hashMapOf(
            "userName" to name,
            "userLabel" to label,
            "introduction" to intro
        )

        fun persist(photoUrl: String?) {
            photoUrl?.let { userData["photoUrl"] = it }

            db.collection("users").document(email)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    val prefs = getSharedPreferences("Profile_$email", MODE_PRIVATE).edit()
                    prefs.putString("userName", name)
                    prefs.putString("userLabel", label)
                    prefs.putString("introduction", intro)
                    if (photoUrl != null) {
                        prefs.putString("photoUrl", photoUrl)
                        prefs.remove("userPhotoBase64")
                    }
                    prefs.apply()

                    Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.profile_save_failed), Toast.LENGTH_SHORT).show()
                }
        }

        val uri = imageUri
        if (uri != null) {
            val ref = storage.reference.child("user_photos/$email.jpg")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    ref.downloadUrl
                }
                .addOnSuccessListener { url -> persist(url.toString()) }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.photo_upload_failed), Toast.LENGTH_SHORT).show()
                    persist(null)
                }
        } else {
            persist(null)
        }
    }
}
