package com.capstonecobconnect.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capstonecobconnect.myapplication.cobblers.Cobblers_Registration
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class Register : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var loadingSpinner: ProgressBar

    private lateinit var passwordEditText: EditText
    private lateinit var repeatpasswordEditText: EditText
    private var isPasswordVisible = false

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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

        repeatpasswordEditText = findViewById(R.id.repeat_password_edit_text)


        repeatpasswordEditText.setOnTouchListener { v, event ->
            val drawableRight = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (repeatpasswordEditText.right - repeatpasswordEditText.compoundDrawables[drawableRight].bounds.width())) {
                    toggleRepeatPasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance()

        // Find views by their IDs
        val fNameTxt = findViewById<EditText>(R.id.fName)
        val lNameTxt = findViewById<EditText>(R.id.lName)
        val emailTxt = findViewById<EditText>(R.id.email_edit_text)
        val passwordTxt = findViewById<EditText>(R.id.password_edit_text)
        val repeatPasswordTxt = findViewById<EditText>(R.id.repeat_password_edit_text)
        val registerButton = findViewById<Button>(R.id.create_account_button)
        val cobRegister = findViewById<TextView>(R.id.register_now_text_view)
        loadingSpinner = findViewById(R.id.loading_spinner)

        cobRegister.setOnClickListener {
            val intent = Intent(this, Cobblers_Registration::class.java)
            startActivity(intent)
            finish()
        }

        // Set onClickListener for the register button
        registerButton.setOnClickListener {
            val fName = fNameTxt.text.toString()
            val lName = lNameTxt.text.toString()
            val email = emailTxt.text.toString()
            val password = passwordTxt.text.toString()
            val repeatPassword = repeatPasswordTxt.text.toString()

            // Validate input fields
            if (fName.isNotEmpty() && lName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && repeatPassword.isNotEmpty()) {
                if (password == repeatPassword) {
                    showLoadingSpinner(true)
                    checkIfEmailExistsAndRegister(fName, lName, email, password)
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoadingSpinner(show: Boolean) {
        loadingSpinner.visibility = if (show) View.VISIBLE else View.GONE
    }

    // Method to check if the email already exists in the database
    private fun checkIfEmailExistsAndRegister(fName: String, lName: String, email: String, password: String) {
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods ?: emptyList()
                if (signInMethods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                    // Email already exists
                    showLoadingSpinner(false)
                    Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
                } else {
                    // Email does not exist, proceed with registration
                    createUserWithEmailAndPassword(fName, lName, email, password)
                }
            } else {
                // Handle errors
                showLoadingSpinner(false)
                Toast.makeText(this, "Failed to check email existence: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Method to create user using Firebase Authentication
    private fun createUserWithEmailAndPassword(fName: String, lName: String, email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoadingSpinner(false)
                if (task.isSuccessful) {
                    // Sign in success
                    val user = mAuth.currentUser

                    user?.let {
                        sendEmailVerification(it) // Send email verification
                        saveUserToDatabase(user?.uid, fName, lName, email)
                        Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                        // Redirect to the login page or any other activity
                        val intent = Intent(this, login::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to send email verification
    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Verification email sent. Please check your email to verify your account.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Redirect to login page after sending email
                    val intent = Intent(this, login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Method to save user data to Firestore
    private fun saveUserToDatabase(userId: String?, fName: String, lName: String, email: String) {
        userId?.let {
            val userRef = mFirestore.collection("customers").document(it)
            val userData = HashMap<String, Any>()
            userData["userId"] = userId
            userData["fName"] = fName
            userData["lName"] = lName
            userData["email"] = email
            userData["address"] = "Not Provided"
            userData["phone"] = "Not Provided"
            userData["profileImg"] = "https://firebasestorage.googleapis.com/v0/b/cobconnectapp.appspot.com/o/default-picture.png?alt=media&token=732cbb5b-a17e-4039-ac98-efa10608843b"
            userRef.set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "User data saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
            passwordEditText.setCompoundDrawables(passwordIcon, null,eyeOffIcon, null)
        }
        isPasswordVisible = !isPasswordVisible
        // Move the cursor to the end of the text
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun toggleRepeatPasswordVisibility() {

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
            repeatpasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            repeatpasswordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password_icon, 0, R.drawable.eye_icon, 0)
        } else {
            // Show password
            repeatpasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            repeatpasswordEditText.setCompoundDrawables(passwordIcon, null,eyeOffIcon, null)
        }
        isPasswordVisible = !isPasswordVisible
        // Move the cursor to the end of the text
        repeatpasswordEditText.setSelection(repeatpasswordEditText.text.length)
    }

    private fun resizeDrawable(drawable: Drawable, width: Int, height: Int): Drawable {
        drawable.setBounds(0, 0, width, height)
        return drawable
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
