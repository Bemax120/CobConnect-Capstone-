package com.capstonecobconnect.myapplication.services

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.capstonecobconnect.myapplication.Home_Page
import com.capstonecobconnect.myapplication.Maps
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Service_Request : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var customerDocumentId: String
    private val LOCATION_REQUEST_CODE = 2
    private val IMAGE_PICK_CODE = 100
    private var exactLoc: String = ""

    private lateinit var listViewImage: ListView
    private lateinit var imageAdapter: ImageAdapter
    private var imageList = mutableListOf<ImageRow>()
    private val imageUriList = mutableListOf<Uri>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_request)

        val userId = intent.getStringExtra("USER_ID")


        firestore = FirebaseFirestore.getInstance()
        customerDocumentId = intent.getStringExtra("customerDocumentId") ?: ""

        val btnSubmit = findViewById<Button>(R.id.submitbtnSR)
        val phoneNum = findViewById<EditText>(R.id.phoneNumSR)
        val issue = findViewById<EditText>(R.id.issueSR)
        val btnCurrentLoc = findViewById<Button>(R.id.btnGetLocation)
        val btnAddImage = findViewById<Button>(R.id.addImageButton)
        val addressEditText = findViewById<EditText>(R.id.addressSR)

        listViewImage = findViewById(R.id.list_item_image)
        imageAdapter = ImageAdapter(this, imageList)
        listViewImage.adapter = imageAdapter

        btnAddImage.setOnClickListener {
            pickImage()
        }

        btnCurrentLoc.setOnClickListener {
            val intent = Intent(this, Maps::class.java)
            startActivityForResult(intent, LOCATION_REQUEST_CODE)
        }

        val selectedServices =
            intent.getSerializableExtra("selected_service_info") as? ArrayList<SelectedServiceInfo>

        btnSubmit.setOnClickListener {
            val phone = phoneNum.text.toString()
            val issueText = issue.text.toString()
            val addressText = addressEditText.text.toString()
            val timestamp = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = Date(timestamp)
            val formattedDate = sdf.format(date)

            if (phone.isEmpty() || issueText.isEmpty() || addressText.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestData = hashMapOf(
                "userId" to userId,
                "phone" to phone,
                "issue" to issueText,
                "address" to addressText,
                "geolocation" to exactLoc,
                "dateShed" to formattedDate,
                "status" to "Pending"
            )

            firestore.collection("Request")
                .add(requestData)
                .addOnSuccessListener { documentRef ->
                    val requestId = documentRef.id

                    // Upload selected services
                    selectedServices?.forEach { info ->
                        val serviceNames = info.services.joinToString(", ") { it.service }
                        val categories = info.services.joinToString(", ") { it.category }
                        val prices = info.services.joinToString(", ") { it.price.toString() }

                        val totalPrice = info.services.sumOf { it.price }

                        val serviceData = hashMapOf(
                            "services" to serviceNames,
                            "categories" to categories,
                            "prices" to prices,
                            "quantity" to info.quantity,
                            "totalPrice" to totalPrice
                        )

                        firestore.collection("Request")
                            .document(requestId)
                            .collection("SelectedServices")
                            .add(serviceData)
                    }

                    // Upload images to Firebase Storage and save URLs
                    uploadImagesToStorage(requestId)

                    val intent = Intent(this, Home_Page::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("NAV_TARGET", "activity")
                    startActivity(intent)
                    finish()



                    Toast.makeText(this, "Submitting request...", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit request", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                imageUriList.add(imageUri)
                addImageToRow(imageUri)
            }
        }

        if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val addressEditText = findViewById<EditText>(R.id.addressSR)

            if (latitude != 0.0 && longitude != 0.0) {
                exactLoc = "$latitude,$longitude"

                val geocoder = Geocoder(this, Locale.getDefault())
                val addressText = runCatching {
                    val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)!!
                    addresses.firstOrNull()?.getAddressLine(0) ?: "No address found"
                }.getOrElse {
                    "Geocoding failed"
                }

                runOnUiThread {
                    addressEditText.setText(addressText)
                }
            } else {
                Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addImageToRow(imageUri: Uri?) {
        if (imageUri == null) return

        var added = false

        for (row in imageList) {
            when {
                row.image1 == null -> {
                    row.image1 = imageUri
                    added = true
                    break
                }
                row.image2 == null -> {
                    row.image2 = imageUri
                    added = true
                    break
                }
                row.image3 == null -> {
                    row.image3 = imageUri
                    added = true
                    break
                }
                row.image4 == null -> {
                    row.image4 = imageUri
                    added = true
                    break
                }
            }
        }

        if (!added) {
            imageList.add(ImageRow(image1 = imageUri))
        }

        imageAdapter.notifyDataSetChanged()
        setListViewHeightBasedOnItemsImage(listViewImage)
    }

    private fun setListViewHeightBasedOnItemsImage(listView: ListView) {
        val adapter = listView.adapter ?: return

        var totalHeight = 0
        for (i in 0 until adapter.count) {
            val listItem = adapter.getView(i, null, listView)
            listItem.measure(
                View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (adapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    private fun uploadImagesToStorage(requestId: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageUrls = mutableListOf<String>()
        var uploadedCount = 0

        if (imageUriList.isEmpty()) {
            Toast.makeText(this, "Request submitted successfully!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imageUriList.forEachIndexed { index, uri ->
            val imageRef = storageRef.child("requestImages/${UUID.randomUUID()}.jpg")
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())
                        uploadedCount++

                        if (uploadedCount == imageUriList.size) {
                            // Save all image URLs to Firestore
                            val imagesData = hashMapOf("imageUrls" to imageUrls)
                            firestore.collection("Request")
                                .document(requestId)
                                .collection("Images")
                                .add(imagesData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Request and images submitted successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
