package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.Change_Password
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.login
import com.google.firebase.firestore.FirebaseFirestore

class Cobbler_Profile_Fragment : Fragment() {

    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var userId: String

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_cobbler__profile_, container, false)

        mFirestore = FirebaseFirestore.getInstance()

        val changePassTV = view.findViewById<TextView>(R.id.changePassCob)
        val techSupportTV = view.findViewById<TextView>(R.id.technicalSupportCob)
        val logout = view.findViewById<Button>(R.id.logOutCob)
        val editBtn = view.findViewById<Button>(R.id.editProfileCob)

        // Retrieve userId from arguments
        userId = arguments?.getString("USER_ID") ?: ""

        editBtn.setOnClickListener {
            val intent = Intent(activity, Cobblers_Edit_Profile::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        logout.setOnClickListener {
            val intent = Intent(activity, login::class.java)
            startActivity(intent)
            activity?.finish()
            Toast.makeText(activity, "Logout Successfully", Toast.LENGTH_SHORT).show()
        }

        changePassTV.setOnClickListener {
            val intent = Intent(activity, Change_Password::class.java)
            startActivity(intent)
        }

        techSupportTV.setOnClickListener {
            val intent = Intent(activity, Cobblers_Technical_Support::class.java)
            startActivity(intent)
        }

        // Retrieve userId from arguments
        val userId = arguments?.getString("USER_ID")

        // Display the userId in a TextView or use it for further operations
        if (userId != null) {
            fetchUserDataFromFirestore(userId, view)
        }

        return view
    }

    // Method to fetch user data from Firestore
    @SuppressLint("SetTextI18n")
    private fun fetchUserDataFromFirestore(userId: String, view: View) {
        val userRef = mFirestore.collection("cobblers").document(userId)

        // Add a real-time listener
        userRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Toast.makeText(activity, "Error fetching user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val fName = snapshot.getString("fName") ?: "N/A"
                val lName = snapshot.getString("lName") ?: "N/A"
                val email = snapshot.getString("email") ?: "N/A"
                val phone = snapshot.getString("phone") ?: "N/A"
                val address = snapshot.getString("address") ?: "N/A"
                val profileImage = snapshot.getString("profileImg") ?: ""

                val fNameTextView = view.findViewById<TextView>(R.id.fullNameCob)
                val emailTextView = view.findViewById<TextView>(R.id.emailCob)
                val profileImageView = view.findViewById<ImageView>(R.id.profileImgCob)
                val phoneTextView = view.findViewById<TextView>(R.id.phoneCob)
                val addressTextView = view.findViewById<TextView>(R.id.addressCob)

                fNameTextView.text = "$fName $lName"
                emailTextView.text = email
                phoneTextView.text = phone
                addressTextView.text = address

                if (profileImage.isNotEmpty()) {
                    // Load image using Glide
                    Glide.with(this@Cobbler_Profile_Fragment)
                        .load(profileImage)
                        .circleCrop()
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.default_picture) // Set a default image if URL is empty
                }
            } else {
                Toast.makeText(activity, "User data not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        // Method to create a new instance of Cobbler_Profile_Fragment with the userId
        fun newInstance(userId: String): Cobbler_Profile_Fragment {
            val fragment = Cobbler_Profile_Fragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }
}