package com.capstonecobconnect.myapplication.customers.feedback

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstonecobconnect.myapplication.R

class Client_Feedback : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_client_feedback)

        // Initialize views
        val feedbackInput = findViewById<EditText>(R.id.feedbackInput)
        val characterCount = findViewById<TextView>(R.id.characterCount)
        val starRating = findViewById<RatingBar>(R.id.starRating)
        val ratingValue = findViewById<TextView>(R.id.ratingValue)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val btnSkip = findViewById<Button>(R.id.btnSkip)

        // Character counter
        feedbackInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                characterCount.text = "$count/500"
                characterCount.setTextColor(if (count > 500) Color.RED else ContextCompat.getColor(this@Client_Feedback, R.color.text_light))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Star rating listener
        starRating.setOnRatingBarChangeListener { _, rating, _ ->
            ratingValue.text = "${rating.toInt()}/5"
        }

        // Button click listeners
        btnSubmit.setOnClickListener {
            // Handle submit action
        }

        btnSkip.setOnClickListener {
            // Handle skip action
        }
    }
}