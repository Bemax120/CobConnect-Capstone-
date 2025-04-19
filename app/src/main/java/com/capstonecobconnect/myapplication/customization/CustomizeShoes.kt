package com.capstonecobconnect.myapplication.customization

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.services.ServiceRequestFormActivity
import com.capstonecobconnect.myapplication.services.Service_Request

class CustomizeShoes : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customize_shoes)

        val custDone = findViewById<Button>(R.id.cust_Done)

        custDone.setOnClickListener {
            val intent = Intent(this, Service_Request::class.java)
            startActivity(intent )
        }



    }
}