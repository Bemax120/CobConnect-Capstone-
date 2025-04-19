package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.login
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class Cobblers_Registration : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var loadingSpinner: ProgressBar

    private lateinit var passwordEditText: EditText
    private lateinit var repeatpasswordEditText: EditText
    private var isPasswordVisible = false

    private lateinit var selectedImageUri: Uri // Variable to hold the selected image URI

    @SuppressLint("MissingInflatedId", "CutPasteId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobblers_registration)

        passwordEditText = findViewById(R.id.COB_password_edit_text)

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

        repeatpasswordEditText = findViewById(R.id.COB_repeat_password_edit_text)

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
        val fNameTxt = findViewById<EditText>(R.id.COB_fName)
        val lNameTxt = findViewById<EditText>(R.id.COB_lName)
        val emailTxt = findViewById<EditText>(R.id.COB_email_edit_text)
        val passwordTxt = findViewById<EditText>(R.id.COB_password_edit_text)
        val repeatPasswordTxt = findViewById<EditText>(R.id.COB_repeat_password_edit_text)
        val registerButton = findViewById<Button>(R.id.COB_create_account_button)
        val uploadButton = findViewById<Button>(R.id.COB_upload_button) // Added upload button
        loadingSpinner = findViewById(R.id.loading_spinner)

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

        // Set onClickListener for the upload button
        uploadButton.setOnClickListener {
            // Open an image picker to allow the user to select a picture
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
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
                if (task.isSuccessful) {
                    // Sign in success
                    val user = mAuth.currentUser
                    saveUserToDatabase(user?.uid, fName, lName, email)
                    Toast.makeText(this, "Cobbler registered successfully", Toast.LENGTH_SHORT).show()
                    // Redirect to the login page or any other activity
                    val intent = Intent(this, login::class.java)
                    startActivity(intent)
                    finish()
                } else {

                    // If sign in fails, display a message to the user.
                    showLoadingSpinner(false)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Method to save user data to Firestore
    private fun saveUserToDatabase(userId: String?, fName: String, lName: String, email: String) {
        userId?.let {
            val userRef = mFirestore.collection("cobblers").document(it)
            val userData = HashMap<String, Any>()
            userData["userId"] = userId
            userData["fName"] = fName
            userData["lName"] = lName
            userData["email"] = email
            userData["address"] = ""
            userData["phone"] = ""
            userData["walletBalance"] = 0
            userData["profileImg"] = "https://firebasestorage.googleapis.com/v0/b/cobconnectapp.appspot.com/o/default-picture.png?alt=media&token=732cbb5b-a17e-4039-ac98-efa10608843b" // Default image URL
            userRef.set(userData)
                .addOnSuccessListener {
                    showLoadingSpinner(false)
                    Toast.makeText(this, "Cobbler data saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    showLoadingSpinner(false)
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
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.password_icon, 0,
                R.drawable.eye_icon, 0)
        } else {
            // Show password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordEditText.setCompoundDrawables(passwordIcon, null, eyeOffIcon, null)
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
            repeatpasswordEditText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.password_icon, 0,
                R.drawable.eye_icon, 0)
        } else {
            // Show password
            repeatpasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            repeatpasswordEditText.setCompoundDrawables(passwordIcon, null, eyeOffIcon, null)
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

    // Method to handle the result of image picker
    // Method to handle the result of image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Save the selected image URI
                selectedImageUri = uri
                // Set the upload button text to display the name of the selected image file
                val fileName = getFileName(uri)
                findViewById<Button>(R.id.COB_upload_button).text = fileName
                // TODO: Upload the selected image to Firebase Storage
            }
        }
    }

    // Method to get the file name from the URI
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow("_display_name"))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown"
    }


    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 100
    }
}
