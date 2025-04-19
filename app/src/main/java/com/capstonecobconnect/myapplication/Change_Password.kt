package com.capstonecobconnect.myapplication

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider

class Change_Password : AppCompatActivity() {

    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmNewPasswordEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var userId: String

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Retrieve the userId from the Intent
        userId = intent.getStringExtra("USER_ID") ?: ""

        auth = FirebaseAuth.getInstance()

        // Initialize views
        oldPasswordEditText = findViewById(R.id.oldPasswordEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPasswordEditText)
        saveButton = findViewById(R.id.saveButton)

        oldPasswordEditText.setOnTouchListener { v, event ->
            val drawableRight = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (oldPasswordEditText.right - oldPasswordEditText.compoundDrawables[drawableRight].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        saveButton.setOnClickListener {
            validateOldPasswordAndChange()
        }
    }

    private fun validateOldPasswordAndChange() {
        val currentUser = auth.currentUser

        // Check if the current user's ID matches the passed userId
        if (currentUser?.uid != userId) {
            Toast.makeText(this, "User authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val oldPassword = oldPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()
        val confirmNewPassword = confirmNewPasswordEditText.text.toString()

        // Check if any field is empty
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please input all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the new password is at least 8 characters long
        if (newPassword.length < 8) {
            Toast.makeText(this, "New password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)

        currentUser.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                if (newPassword == confirmNewPassword) {
                    currentUser.updatePassword(newPassword).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePasswordVisibility() {
        val eyeOffIcon = resizeDrawable(
            ContextCompat.getDrawable(this, R.drawable.eye)!!,
            dpToPx(24),
            dpToPx(24)
        )

        if (isPasswordVisible) {
            // Hide password
            oldPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            newPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            confirmNewPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            oldPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye_icon, 0)
        } else {
            // Show password
            oldPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            newPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            confirmNewPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            oldPasswordEditText.setCompoundDrawables(null, null, eyeOffIcon, null)
        }
        isPasswordVisible = !isPasswordVisible
        // Move the cursor to the end of the text
        oldPasswordEditText.setSelection(oldPasswordEditText.text.length)
        newPasswordEditText.setSelection(newPasswordEditText.text.length)
        confirmNewPasswordEditText.setSelection(confirmNewPasswordEditText.text.length)
    }

    private fun resizeDrawable(drawable: Drawable, width: Int, height: Int): Drawable {
        drawable.setBounds(0, 0, width, height)
        return drawable
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
