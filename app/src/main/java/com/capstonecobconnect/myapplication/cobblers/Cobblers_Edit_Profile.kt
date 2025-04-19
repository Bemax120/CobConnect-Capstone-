package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

@Suppress("DEPRECATION")
class Cobblers_Edit_Profile : AppCompatActivity() {
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var progressBar: ProgressBar
    private lateinit var displayImg: ImageView
    private var imageUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 101
    }

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobblers_edit_profile)

        mFirestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID is empty", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val backbtn = findViewById<ImageView>(R.id.editBackCob)
        val fNameEditText = findViewById<EditText>(R.id.editFnameCob)
        val lNameEditText = findViewById<EditText>(R.id.editLnameCob)
        val addressEditText = findViewById<EditText>(R.id.editAddressCob)
        val phoneEditText = findViewById<EditText>(R.id.editContactCob)
        val saveBtn = findViewById<Button>(R.id.editSave_btnCob)
        val uploadpp = findViewById<Button>(R.id.editUpload_btnCob)
        displayImg = findViewById(R.id.editDisplayImgCob)
        progressBar = findViewById(R.id.loading_spinner)

        loadUserData(userId, fNameEditText, lNameEditText, addressEditText, phoneEditText)

        uploadpp.setOnClickListener {
            openGallery()
        }

        backbtn.setOnClickListener {
            finish()
        }

        saveBtn.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val updatedData = hashMapOf(
                "fName" to fNameEditText.text.toString(),
                "lName" to lNameEditText.text.toString(),
                "address" to addressEditText.text.toString(),
                "phone" to phoneEditText.text.toString()
            )
            updateUserData(userId, updatedData)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                imageUri = uri
                Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(displayImg)
                // DO NOT upload image here! Wait for Save button.
            }
        }
    }

    private fun uploadImageAndFinish(userId: String) {
        if (imageUri == null) {
            // No image selected, just finish
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            goBackToAccountInfo()
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("profileImages/${UUID.randomUUID()}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    updateUserProfileImage(userId, imageUrl)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileImage(userId: String, imageUrl: String) {
        val userRef = mFirestore.collection("cobblers").document(userId)
        userRef.update("profileImg", imageUrl)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                goBackToAccountInfo()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update profile image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData(
        userId: String,
        fNameEditText: EditText,
        lNameEditText: EditText,
        addressEditText: EditText,
        phoneEditText: EditText
    ) {
        progressBar.visibility = View.VISIBLE
        val userRef = mFirestore.collection("cobblers").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    val userData = document.data
                    fNameEditText.setText(userData?.get("fName") as? String ?: "")
                    lNameEditText.setText(userData?.get("lName") as? String ?: "")
                    addressEditText.setText(userData?.get("address") as? String ?: "")
                    phoneEditText.setText(userData?.get("phone") as? String ?: "")
                    val profileImgUrl = userData?.get("profileImg") as? String
                    if (!profileImgUrl.isNullOrEmpty()) {
                        Glide.with(this).load(profileImgUrl).circleCrop().into(displayImg)
                    }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserData(userId: String, updatedData: Map<String, Any>) {
        val userRef = mFirestore.collection("cobblers").document(userId)
        userRef.update(updatedData)
            .addOnSuccessListener {
                uploadImageAndFinish(userId)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goBackToAccountInfo() {
        // Return back to Account Information
        setResult(Activity.RESULT_OK)
        finish()
    }
}