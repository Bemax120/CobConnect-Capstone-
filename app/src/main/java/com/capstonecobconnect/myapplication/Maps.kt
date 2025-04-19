package com.capstonecobconnect.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FetchPlaceRequest




class Maps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var selectedLocation: LatLng? = null

    private lateinit var placesClient: PlacesClient
    private lateinit var searchPlace: EditText
    private lateinit var sclv: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val placeSuggestions = mutableListOf<String>()


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val getLoc = findViewById<Button>(R.id.get_location_button)

        searchPlace = findViewById(R.id.searchPlace)
        listView = findViewById(R.id.lv_places)
        sclv = findViewById(R.id.sv_lv)
        progressBar = findViewById(R.id.pb_lv)

        listView.isVerticalScrollBarEnabled = true
        listView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY


        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDIc0jC5YzmWFMWKa6OqdTHaq3hIaqbeWw") // Replace with your API key
        }
        placesClient = Places.createClient(this)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, placeSuggestions)
        listView.adapter = adapter




        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = placeSuggestions[position]
            searchPlace.setText(selectedPlace)

            progressBar.visibility = View.GONE
            sclv.visibility = View.GONE


            // Geocode the selected place and place a pin on the map
            getPlaceCoordinates(selectedPlace)
        }

        // Listen to search text changes
        searchPlace.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                progressBar.visibility = View.VISIBLE

                if (!s.isNullOrEmpty()) {
                    progressBar.visibility = View.GONE
                    sclv.visibility = View.VISIBLE
                    searchPlaces(s.toString())
                } else {
                    progressBar.visibility = View.GONE
                    sclv.visibility = View.GONE

                }

                if (!s.isNullOrEmpty()) {
                    searchPlaces(s.toString())
                }
            }
        })



        getLoc.setOnClickListener {
            if (selectedLocation != null) {
                returnLocation(selectedLocation!!)
            } else {
                getCurrentLocation()
            }
        }

        // Initialize the SupportMapFragment and request notification when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.uiSettings.isZoomControlsEnabled = true

        if (checkLocationPermission()) {
            gMap.isMyLocationEnabled = true
            getCurrentLocation()  // Get the current location once the map is ready
            setMapClickListener()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            false
        } else {
            true
        }
    }

    private fun setMapClickListener() {
        gMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            gMap.clear()
            gMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    gMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                    selectedLocation = currentLatLng // Automatically select the current location
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun returnLocation(location: LatLng) {
        val intent = Intent()
        intent.putExtra("latitude", location.latitude)
        intent.putExtra("longitude", location.longitude)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            } else {
                Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchPlaces(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            placeSuggestions.clear()
            for (prediction: AutocompletePrediction in response.autocompletePredictions) {
                placeSuggestions.add(prediction.getPrimaryText(null).toString())
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPlaceCoordinates(placeName: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(placeName)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            if (response.autocompletePredictions.isNotEmpty()) {
                val placeId = response.autocompletePredictions[0].placeId

                val placeRequest = com.google.android.libraries.places.api.net.FetchPlaceRequest.builder(
                    placeId,
                    listOf(com.google.android.libraries.places.api.model.Place.Field.LAT_LNG)
                ).build()

                placesClient.fetchPlace(placeRequest).addOnSuccessListener { fetchResponse ->
                    val place = fetchResponse.place
                    val latLng = place.latLng
                    if (latLng != null) {
                        selectedLocation = latLng
                        gMap.clear()
                        gMap.addMarker(MarkerOptions().position(latLng).title(placeName))
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


}