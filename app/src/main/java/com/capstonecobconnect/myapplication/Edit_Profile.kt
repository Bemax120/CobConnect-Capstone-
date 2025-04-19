package com.capstonecobconnect.myapplication

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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class Edit_Profile : AppCompatActivity() {

    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var progressBar: ProgressBar
    private lateinit var displayImg: ImageView
    private var imageUri: Uri? = null

    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 101
    }

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        mFirestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("USER_ID") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID is empty", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val backbtn = findViewById<ImageView>(R.id.editBack)
        val fNameEditText = findViewById<EditText>(R.id.editFname)
        val lNameEditText = findViewById<EditText>(R.id.editLname)
        val addressEditText = findViewById<EditText>(R.id.editAddress)
        val phoneEditText = findViewById<EditText>(R.id.editContact)
        val saveBtn = findViewById<Button>(R.id.editSave_btn)
        val uploadpp = findViewById<Button>(R.id.editUpload_btn)

        displayImg = findViewById<ImageView>(R.id.editDisplayImg)
        progressBar = findViewById(R.id.loading_spinner)

        loadUserData(userId, fNameEditText, lNameEditText, addressEditText, phoneEditText)

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
            if (imageUri != null) {
                uploadImageToFirestore(imageUri!!) { imageUrl ->
                    updatedData["profileImg"] = imageUrl
                    updateUserData(userId, updatedData)
                }
            } else {
                updateUserData(userId, updatedData)
            }
        }

        uploadpp.setOnClickListener {
            openGallery()
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
            }
        }
    }

    private fun uploadImageToFirestore(imageUri: Uri, onSuccess: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("profileImages/${UUID.randomUUID()}.jpg")

        fileRef.putFile(imageUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserData(userId: String, updatedData: Map<String, Any>) {
        val userRef = mFirestore.collection("customers").document(userId)
        userRef.update(updatedData)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val userRef = mFirestore.collection("customers").document(userId)
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
                    if (profileImgUrl != null) {
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
}
