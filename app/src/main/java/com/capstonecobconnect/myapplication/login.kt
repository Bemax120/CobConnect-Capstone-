package com.capstonecobconnect.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capstonecobconnect.myapplication.cobblers.Cobblers_Home
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class login : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var loadingSpinner: ProgressBar

    private lateinit var passwordEditText: EditText
    private var isPasswordVisible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val forgetPassTxt = findViewById<TextView>(R.id.forgot_password_text_view)


        forgetPassTxt.setOnClickListener {
            val intent = Intent(this, Forget_Password::class.java)
            startActivity(intent)
        }

        passwordEditText = findViewById(R.id.password_edit_text)

        passwordEditText.setOnTouchListener { v, event ->
            val drawableRight = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[drawableRight].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance()

        // Find the login button and other views
        val loginButton = findViewById<Button>(R.id.login_button)
        val createAccTxt = findViewById<TextView>(R.id.create_accTxt)
        loadingSpinner = findViewById(R.id.loading_spinner)

        createAccTxt.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for the login button
        loginButton.setOnClickListener {
            performLogin()
        }

        // Trigger login on Enter key press
        passwordEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                performLogin()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun performLogin() {
        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Show the loading spinner
        loadingSpinner.visibility = View.VISIBLE

        // Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    user?.let {
                        if (isEmailVerified(it)) {
                            findUserCollection(it.uid)
                        } else {
                            Toast.makeText(
                                this, "Please verify your email before logging in.", Toast.LENGTH_LONG
                            ).show()
                            showResendVerificationDialog(it)
                            mAuth.signOut() // Sign out unverified user
                            loadingSpinner.visibility = View.GONE
                        }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Authentication failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    passwordEditText.text.clear()
                    loadingSpinner.visibility = View.GONE
                }
            }
    }

    private fun showResendVerificationDialog(user: FirebaseUser) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email Not Verified")
        builder.setMessage("Your email is not verified. Would you like to resend the verification email?")

        builder.setPositiveButton("Resend") { _, _ ->
            user.sendEmailVerification()
                .addOnSuccessListener {
                    Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }


    // Method to find which collection the user belongs to
    private fun findUserCollection(userId: String) {
        val userRefcus = mFirestore.collection("customers").document(userId)
        val userRefcob = mFirestore.collection("cobblers").document(userId)

        userRefcus.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    fetchUserDataFromFirestoreCus(userId, "customers")
                } else {
                    userRefcob.get()
                        .addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                fetchUserDataFromFirestoreCob(userId, "cobblers")
                            } else {
                                Toast.makeText(this, "User data not found in any collection", Toast.LENGTH_SHORT).show()
                                // Hide the loading spinner
                                loadingSpinner.visibility = View.GONE
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Hide the loading spinner
                            loadingSpinner.visibility = View.GONE
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_SHORT).show()
                // Hide the loading spinner
                loadingSpinner.visibility = View.GONE
            }
    }

    // Method to fetch user data from Firestore for customers
    private fun fetchUserDataFromFirestoreCus(userId: String, collection: String) {
        val userRef = mFirestore.collection(collection).document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fName = document.getString("fName") ?: "User"
                    val lName = document.getString("lName") ?: "User"
                    Toast.makeText(this, "Welcome back, $fName $lName", Toast.LENGTH_SHORT).show()
                    // Redirect to home page with userId
                    val intent = Intent(this, Home_Page::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
                // Hide the loading spinner
                loadingSpinner.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_SHORT).show()
                // Hide the loading spinner
                loadingSpinner.visibility = View.GONE
            }
    }

    // Method to fetch user data from Firestore for cobblers
    private fun fetchUserDataFromFirestoreCob(userId: String, collection: String) {
        val userRef = mFirestore.collection(collection).document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fName = document.getString("fName") ?: "User"
                    val lName = document.getString("lName") ?: "User"
                    Toast.makeText(this, "Welcome back, $fName $lName", Toast.LENGTH_SHORT).show()
                    // Redirect to home page with userId
                    val intent = Intent(this, Cobblers_Home::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
                // Hide the loading spinner
                loadingSpinner.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_SHORT).show()
                // Hide the loading spinner
                loadingSpinner.visibility = View.GONE
            }
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        if (!doubleBackToExitPressedOnce) {
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        } else {
            finish() // Close the activity
        }
    }

    private fun togglePasswordVisibility() {
        val passwordIcon = resizeDrawable(
            ContextCompat.getDrawable(this, R.drawable.password_icon)!!,
            dpToPx(24),
            dpToPx(24)
        )

        val eyeOffIcon = resizeDrawable(
            ContextCompat.getDrawable(this, R.drawable.eye)!!,
            dpToPx(24),
            dpToPx(24)
        )

        if (isPasswordVisible) {
            // Hide password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password_icon, 0, R.drawable.eye_icon, 0)
        } else {
            // Show password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordEditText.setCompoundDrawables(passwordIcon, null, eyeOffIcon, null)
        }
        isPasswordVisible = !isPasswordVisible
        // Move the cursor to the end of the text
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun resizeDrawable(drawable: Drawable, width: Int, height: Int): Drawable {
        drawable.setBounds(0, 0, width, height)
        return drawable
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // Function to check if the user's email is verified
    private fun isEmailVerified(user: FirebaseUser): Boolean {
        return user.isEmailVerified
    }

}
