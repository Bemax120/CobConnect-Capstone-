package com.capstonecobconnect.myapplication.cobblers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.capstonecobconnect.myapplication.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

@Suppress("DEPRECATION")
class Booking_Page : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TAG = "Booking_Page"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var documentId: String

    private var backPressedTime: Long = 0
    private lateinit var toast: Toast

    private lateinit var showArriveBtn: LinearLayout
    private lateinit var btnAccept: Button


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_page)

        // Retrieve documentId from Intent
        documentId = intent.getStringExtra("documentId") ?: ""

        showArriveBtn = findViewById(R.id.layoutArrive)
        btnAccept = findViewById(R.id.btn_accept)
        val reselectBtn = findViewById<Button>(R.id.btn_reselect)

        btnAccept.setOnClickListener {
            btnAccept.visibility = View.GONE
            reselectBtn.visibility = View.GONE
            showArriveBtn.visibility = View.VISIBLE

            val userId = auth.currentUser?.uid

            if (!userId.isNullOrEmpty() && documentId.isNotEmpty()) {
                val requestDocRef = firestore.collection("Request").document(documentId)

                requestDocRef.get()
                    .addOnSuccessListener { requestSnapshot ->
                        if (requestSnapshot.exists()) {
                            val customerId = requestSnapshot.getString("userId") ?: ""

                            // Send a notification to the customer
                            val notificationData = mapOf(
                                "title" to "CobConnect Update",
                                "body" to "Cobblers Accept Your Request Wilson",
                                "receiverId" to customerId,
                                "timestamp" to System.currentTimeMillis()
                            )

                            firestore.collection("notifications")
                                .add(notificationData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "📬 Notification sent to customerId: $customerId")
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "❌ Failed to send notification", e)
                                }
                        } else {
                            Log.w(TAG, "❌ Request document $documentId does not exist.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "❌ Failed to retrieve request for notification", e)
                    }
            } else {
                Log.w(TAG, "❌ Missing userId or documentId")
            }

        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapBooking) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val arriveBtn = findViewById<Button>(R.id.btn_arrive)
        arriveBtn.setOnClickListener {
            handleArriveButtonClick()
        }


        reselectBtn.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null && documentId.isNotEmpty()) {
                firestore.collection("Request").document(documentId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            firestore.collection("Request").document(documentId)
                                .update("status", "Pending")
                                .addOnSuccessListener {
                                    Log.d(TAG, "Status updated to Pending for document $documentId")
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error updating status to Pending", e)
                                }
                        } else {
                            Log.w(TAG, "Document $documentId does not exist, cannot update status.")
                        }
                    }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.uiSettings.isZoomControlsEnabled = true

        if (checkLocationPermission()) {
            initializeMap()
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun initializeMap() {
        if (!checkLocationPermission()) return

        gMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                Log.d(TAG, "Current Location: ${currentLatLng.latitude}, ${currentLatLng.longitude}")
                gMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))

                val geoString = intent.getStringExtra("geoCoding")
                geoString?.let {
                    val parts = it.split(",")
                    if (parts.size == 2) {
                        val destLat = parts[0].toDouble()
                        val destLon = parts[1].toDouble()
                        val addressLatLng = LatLng(destLat, destLon)
                        Log.d(TAG, "Destination Location: ${destLat}, ${destLon}")

                        gMap.addMarker(MarkerOptions().position(addressLatLng).title("Exact Location"))

                        val boundsBuilder = LatLngBounds.Builder()
                        boundsBuilder.include(currentLatLng)
                        boundsBuilder.include(addressLatLng)

                        val bounds = boundsBuilder.build()
                        val padding = 200
                        gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

                        drawRoute(currentLatLng, addressLatLng)
                    } else {
                        Log.e(TAG, "Invalid destination coordinates format!")
                    }
                }
            } ?: Log.e(TAG, "Current location is null!")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get current location: ${e.message}")
        }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val apiKey = "AIzaSyDIc0jC5YzmWFMWKa6OqdTHaq3hIaqbeWw"
        val mode = "driving"
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=$mode&key=$apiKey"

        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e(TAG, "Directions API call failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body
                if (responseBody != null) {
                    val jsonData = responseBody.string()
                    Log.d(TAG, "Directions API Response: $jsonData")

                    try {
                        val jsonObject = JSONObject(jsonData)
                        val routes = jsonObject.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val legs = routes.getJSONObject(0).getJSONArray("legs")
                            val steps = legs.getJSONObject(0).getJSONArray("steps")

                            val path = ArrayList<LatLng>()
                            for (i in 0 until steps.length()) {
                                val polyline = steps.getJSONObject(i)
                                    .getJSONObject("polyline")
                                    .getString("points")
                                path.addAll(decodePoly(polyline))
                            }

                            runOnUiThread {
                                val polylineOptions = PolylineOptions()
                                    .addAll(path)
                                    .width(12f)
                                    .color(Color.BLUE)
                                    .geodesic(true)

                                gMap.addPolyline(polylineOptions)
                            }
                        } else {
                            Log.e(TAG, "No routes found in Directions API response.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Directions API response: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Response body is null.")
                }
            }
        })
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val point = LatLng(lat / 1E5, lng / 1E5)
            poly.add(point)
        }
        return poly
    }

    private fun handleArriveButtonClick() {
        val statusOptions = arrayOf("Complete", "Ongoing", "Cancel")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Update Status")
        builder.setItems(statusOptions) { _, which ->
            val selectedStatus = statusOptions[which]

            val userId = auth.currentUser?.uid

            if (!userId.isNullOrEmpty() && documentId.isNotEmpty()) {
                val requestDocRef = firestore.collection("Request").document(documentId)

                requestDocRef.update("status", selectedStatus)
                    .addOnSuccessListener {
                        Log.d(TAG, "Status updated to $selectedStatus")

                        requestDocRef.get()
                            .addOnSuccessListener { requestSnapshot ->
                                if (requestSnapshot.exists()) {
                                    val requestData = mutableMapOf<String, Any>()
                                    requestSnapshot.data?.forEach { (key, value) ->
                                        if (value != null) requestData[key] = value
                                    }

                                    val customerId = requestSnapshot.getString("userId") ?: ""
                                    requestData["cobblerId"] = userId
                                    requestData["timestamp"] = System.currentTimeMillis()

                                    requestDocRef.collection("Images").get()
                                        .continueWithTask { imageTask ->
                                            if (imageTask.isSuccessful) {
                                                val imagesList = imageTask.result?.documents?.mapNotNull { it.data }
                                                if (!imagesList.isNullOrEmpty()) {
                                                    requestData["images"] = imagesList
                                                }
                                            }
                                            requestDocRef.collection("SelectedServices").get()
                                        }
                                        .addOnSuccessListener { serviceSnapshot ->
                                            val servicesList = serviceSnapshot.documents.mapNotNull { it.data }
                                            if (servicesList.isNotEmpty()) {
                                                requestData["selectedServices"] = servicesList
                                            }

                                            var globalSuccess = false
                                            var customerSuccess = false
                                            var cobblerSuccess = false

                                            val checkAndDeleteRequest = {
                                                if (globalSuccess && customerSuccess && cobblerSuccess) {
                                                    requestDocRef.delete()
                                                        .addOnSuccessListener {
                                                            Log.d(TAG, "✅ Request deleted after saving to activityHistory")
                                                            val intent = Intent(this, Cobblers_Home::class.java)
                                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                            startActivity(intent)
                                                            finish()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.w(TAG, "❌ Failed to delete request document", e)
                                                            Toast.makeText(this, "Failed to delete request.", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                            }

                                            // Save to global activity history
                                            firestore.collection("activityHistory")
                                                .add(requestData)
                                                .addOnSuccessListener {
                                                    globalSuccess = true
                                                    checkAndDeleteRequest()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(TAG, "❌ Failed to save global activity", e)
                                                    Toast.makeText(this, "Failed to save activity history.", Toast.LENGTH_SHORT).show()
                                                }

                                            // Save to customer history
                                            firestore.collection("customers")
                                                .document(customerId)
                                                .collection("activityHistory")
                                                .add(requestData)
                                                .addOnSuccessListener {
                                                    customerSuccess = true
                                                    checkAndDeleteRequest()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(TAG, "❌ Failed to save customer activity", e)
                                                    Toast.makeText(this, "Failed to save activity history.", Toast.LENGTH_SHORT).show()
                                                }

                                            // Save to cobbler history & increment jobCom
                                            val cobblerRef = firestore.collection("cobblers").document(userId)
                                            firestore.runTransaction { transaction ->
                                                transaction.update(cobblerRef, "jobCom", FieldValue.increment(1))
                                                transaction.set(
                                                    cobblerRef.collection("activityHistory").document(),
                                                    requestData
                                                )
                                            }.addOnSuccessListener {
                                                cobblerSuccess = true
                                                checkAndDeleteRequest()
                                            }.addOnFailureListener { e ->
                                                Log.w(TAG, "❌ Failed to save cobbler activity", e)
                                                Toast.makeText(this, "Failed to save activity history.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(TAG, "❌ Failed to fetch SelectedServices", e)
                                            Toast.makeText(this, "Failed to fetch services.", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Log.w(TAG, "❌ Request document $documentId does not exist.")
                                    Toast.makeText(this, "Request no longer exists.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "❌ Failed to get request document", e)
                                Toast.makeText(this, "Failed to retrieve request.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "❌ Failed to update status", e)
                        Toast.makeText(this, "Failed to update status.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User ID or Request ID missing", Toast.LENGTH_SHORT).show()
            }
        }

        builder.show()
    }






    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            toast.cancel()
            super.onBackPressed()
        } else {
            toast = Toast.makeText(this, "Please click BACK again to go back to Home Page", Toast.LENGTH_SHORT)
            toast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMap()
            } else {
                Log.w(TAG, "Location permission denied.")
            }
        }
    }
}
