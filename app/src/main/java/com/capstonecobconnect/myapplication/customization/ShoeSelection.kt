package com.capstonecobconnect.myapplication.customization

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.capstonecobconnect.myapplication.R

class ShoeSelection : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shoe_selection)

        val cCapture = findViewById<ImageButton>(R.id.capture_button)
        val backButton = findViewById<Button>(R.id.back_button)

        // Click listener for the capture button
        cCapture.setOnClickListener {
            // If you have a separate camera activity, change this to CameraCaptureActivity::class.java
            val intent = Intent(this, CustomizeShoes::class.java) // Change this to the correct activity
            startActivity(intent)
        }

        // Optional back button to return to previous screen
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
