package com.capstonecobconnect.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Forget_Password : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        auth = FirebaseAuth.getInstance()

        val emailTxt = findViewById<EditText>(R.id.email_edit_textFP)
        val resetBtn = findViewById<Button>(R.id.reset_password_button)
        val backBtn = findViewById<ImageView>(R.id.backBtnForget)

        backBtn.setOnClickListener {
            startActivity(Intent(this, login::class.java))
            finish()
        }

        resetBtn.setOnClickListener {
            val email = emailTxt.text.toString().trim()

            if (email.isEmpty()) {
                emailTxt.error = "Email is required"
                emailTxt.requestFocus()
                return@setOnClickListener
            }

            resetPassword(email)
        }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to send password reset email. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
